/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.classes

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.SimpleModificationTracker
import com.intellij.psi.*
import com.intellij.psi.augment.PsiAugmentProvider
import com.intellij.psi.impl.PsiCachedValueImpl
import com.intellij.psi.impl.PsiClassImplUtil
import com.intellij.psi.impl.PsiImplUtil
import com.intellij.psi.impl.light.LightMethod
import com.intellij.psi.impl.source.PsiExtensibleClass
import com.intellij.psi.scope.ElementClassHint
import com.intellij.psi.scope.NameHint
import com.intellij.psi.scope.PsiScopeProcessor
import com.intellij.psi.util.CachedValueProvider
import com.intellij.util.ArrayUtil
import gnu.trove.THashMap
import org.jetbrains.kotlin.utils.SmartList
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock

class KotlinClassInnerStuffCache(val myClass: PsiExtensibleClass, externalDependencies: List<Any>) {
    private val myTracker = SimpleModificationTracker()
    private val dependencies: List<Any> = externalDependencies + myTracker

    fun <T : Any> get(initializer: () -> T) = object : Lazy<T> {
        private val lock = ReentrantLock()
        private val holder = lazyPub {
            PsiCachedValueImpl(PsiManager.getInstance(myClass.project),
                               CachedValueProvider<T> {
                                   val v = initializer()
                                   CachedValueProvider.Result.create(v, dependencies)
                               })
        }

        private fun computeValue(): T = holder.value.value ?: error("holder has not null in initializer")

        override val value: T
            get() {
                return if (holder.value.hasUpToDateValue()) {
                    computeValue()
                } else {
                    // the idea behind this locking approach:
                    // Thread T1 starts to calculate value for A it acquires lock for A
                    //
                    // Assumption 1: Lets say A calculation requires another value e.g. B to be calculated
                    // Assumption 2: Thread T2 wants to calculate value for B

                    // to avoid dead-lock
                    // - we mark thread as doing calculation and acquire lock only once per thread
                    // as a trade-off to prevent dependent value could be calculated several time
                    // due to CAS (within putUserDataIfAbsent etc) the same instance of calculated value will be used

                    // TODO: NOTE: acquire lock for a several seconds to avoid dead-lock via resolve is a WORKAROUND

                    if (!initIsRunning.get() && lock.tryLock(5, TimeUnit.SECONDS)) {
                        try {
                            initIsRunning.set(true)
                            try {
                                computeValue()
                            } finally {
                                initIsRunning.set(false)
                            }
                        } finally {
                            lock.unlock()
                        }
                    } else {
                        computeValue()
                    }
                }
            }

        override fun isInitialized() = holder.isInitialized()
    }

    private val _getConstructors: Array<PsiMethod> by get { PsiImplUtil.getConstructors(myClass) }

    val constructors: Array<PsiMethod>
        get() = _getConstructors

    private val _getFields: Array<PsiField> by get { this.getAllFields() }

    val fields: Array<PsiField>
        get() = _getFields

    private val _getMethods: Array<PsiMethod> by get { this.getAllMethods() }

    val methods: Array<PsiMethod>
        get() = _getMethods

    private val _getAllInnerClasses: Array<PsiClass> by get { this.getAllInnerClasses() }

    val innerClasses: Array<out PsiClass>
        get() = _getAllInnerClasses

    private val _getFieldsMap: Map<String, PsiField> by get { this.getFieldsMap() }

    fun findFieldByName(name: String, checkBases: Boolean): PsiField? {
        return if (checkBases) {
            PsiClassImplUtil.findFieldByName(myClass, name, true)
        } else {
            _getFieldsMap[name]
        }
    }

    private val _getMethodsMap: Map<String, Array<PsiMethod>> by get { this.getMethodsMap() }

    fun findMethodsByName(name: String, checkBases: Boolean): Array<PsiMethod> {
        return if (checkBases) {
            PsiClassImplUtil.findMethodsByName(myClass, name, true)
        } else {
            copy(_getMethodsMap[name] ?: PsiMethod.EMPTY_ARRAY)
        }
    }

    private val _getInnerClassesMap: Map<String, PsiClass> by get { this.getInnerClassesMap() }

    fun findInnerClassByName(name: String, checkBases: Boolean): PsiClass? {
        return if (checkBases) {
            PsiClassImplUtil.findInnerByName(myClass, name, true)
        } else {
            _getInnerClassesMap[name]
        }
    }

    private val _makeValuesMethod: PsiMethod by get { this.makeValuesMethod() }

    fun getValuesMethod(): PsiMethod? = if (myClass.isEnum && myClass.name != null) _makeValuesMethod else null

    private val _makeValueOfMethod: PsiMethod by get { this.makeValueOfMethod() }

    fun getValueOfMethod(): PsiMethod? = if (myClass.isEnum && myClass.name != null) _makeValueOfMethod else null

    private fun <T> copy(value: Array<T>): Array<T> {
        return if (value.isEmpty()) value else value.clone()
    }

    private fun getAllFields(): Array<PsiField> {
        val own = myClass.ownFields
        val ext = PsiAugmentProvider.collectAugments(myClass, PsiField::class.java)
        return ArrayUtil.mergeCollections(own, ext, PsiField.ARRAY_FACTORY)
    }

    private fun getAllMethods(): Array<PsiMethod> {
        val own = myClass.ownMethods
        val ext = PsiAugmentProvider.collectAugments(myClass, PsiMethod::class.java)
        return ArrayUtil.mergeCollections(own, ext, PsiMethod.ARRAY_FACTORY)
    }

    private fun getAllInnerClasses(): Array<PsiClass> {
        val own = myClass.ownInnerClasses
        val ext = PsiAugmentProvider.collectAugments(myClass, PsiClass::class.java)
        return ArrayUtil.mergeCollections(own, ext, PsiClass.ARRAY_FACTORY)
    }

    private fun getFieldsMap(): Map<String, PsiField> {
        val fields = this.fields
        if (fields.isEmpty()) return emptyMap()

        val cachedFields = THashMap<String, PsiField>()
        for (field in fields) {
            val name = field.name
            if (!cachedFields.containsKey(name)) {
                cachedFields[name] = field
            }
        }
        return cachedFields
    }

    private fun getMethodsMap(): Map<String, Array<PsiMethod>> {
        val methods = this.methods
        if (methods.isEmpty()) return emptyMap()

        val collectedMethods = hashMapOf<String, MutableList<PsiMethod>>()
        for (method in methods) {
            var list: MutableList<PsiMethod>? = collectedMethods[method.name]
            if (list == null) {
                list = SmartList()
                collectedMethods[method.name] = list
            }
            list.add(method)
        }

        val cachedMethods = THashMap<String, Array<PsiMethod>>()
        for ((key, list) in collectedMethods) {
            cachedMethods[key] = list.toTypedArray()
        }
        return cachedMethods
    }

    private fun getInnerClassesMap(): Map<String, PsiClass> {
        val classes = this.innerClasses
        if (classes.isEmpty()) return emptyMap()

        val cachedInners = THashMap<String, PsiClass>()
        for (psiClass in classes) {
            val name = psiClass.name
            if (name == null) {
                Logger.getInstance(KotlinClassInnerStuffCache::class.java).error(psiClass)
            } else if (psiClass !is ExternallyDefinedPsiElement || !cachedInners.containsKey(name)) {
                cachedInners[name] = psiClass
            }
        }
        return cachedInners
    }

    private fun makeValuesMethod(): PsiMethod {
        return getSyntheticMethod("public static " + myClass.name + "[] values() { }")
    }

    private fun makeValueOfMethod(): PsiMethod {
        return getSyntheticMethod("public static " + myClass.name + " valueOf(java.lang.String name) throws java.lang.IllegalArgumentException { }")
    }

    private fun getSyntheticMethod(text: String): PsiMethod {
        val factory = JavaPsiFacade.getElementFactory(myClass.project)
        val method = factory.createMethodFromText(text, myClass)
        return object : LightMethod(myClass.manager, method, myClass) {
            override fun getTextOffset(): Int {
                return myClass.textOffset
            }
        }
    }

    fun dropCaches() {
        myTracker.incModificationCount()
    }

    companion object {
        private const val VALUES_METHOD = "values"
        private const val VALUE_OF_METHOD = "valueOf"

        @JvmStatic
        private val initIsRunning: ThreadLocal<Boolean> = ThreadLocal.withInitial { false }

        // Copy of PsiClassImplUtil.processDeclarationsInEnum for own cache class
        @JvmStatic
        fun processDeclarationsInEnum(
            processor: PsiScopeProcessor,
            state: ResolveState,
            innerStuffCache: KotlinClassInnerStuffCache
        ): Boolean {
            val classHint = processor.getHint(ElementClassHint.KEY)
            if (classHint == null || classHint.shouldProcess(ElementClassHint.DeclarationKind.METHOD)) {
                val nameHint = processor.getHint(NameHint.KEY)
                if (nameHint == null || VALUES_METHOD == nameHint.getName(state)) {
                    val method = innerStuffCache.getValuesMethod()
                    if (method != null && !processor.execute(method, ResolveState.initial())) return false
                }
                if (nameHint == null || VALUE_OF_METHOD == nameHint.getName(state)) {
                    val method = innerStuffCache.getValueOfMethod()
                    if (method != null && !processor.execute(method, ResolveState.initial())) return false
                }
            }

            return true
        }
    }
}