/*
 * Copyright 2000-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger

import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.search.GlobalSearchScope
import com.sun.jdi.ClassType
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.idea.caches.resolve.util.getJavaClassDescriptor
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import com.sun.jdi.Type as JdiType
import org.jetbrains.org.objectweb.asm.Type as AsmType

fun JdiType.isSubtype(className: String): Boolean = isSubtype(AsmType.getObjectType(className))

fun JdiType.isSubtype(type: AsmType): Boolean {
    if (this.signature() == type.descriptor) {
        return true
    }

    if (type.sort != AsmType.OBJECT || this !is ClassType) {
        return false
    }

    val superTypeName = type.className

    if (allInterfaces().any { it.name() == superTypeName }) {
        return true
    }

    var superClass = superclass()
    while (superClass != null) {
        if (superClass.name() == superTypeName) {
            return true
        }
        superClass = superClass.superclass()
    }

    return false
}

fun AsmType.getClassDescriptor(
    scope: GlobalSearchScope,
    mapBuiltIns: Boolean = true,
    moduleDescriptor: ModuleDescriptor = DefaultBuiltIns.Instance.builtInsModule
): ClassDescriptor? {
    if (AsmUtil.isPrimitive(this)) return null

    val jvmName = JvmClassName.byInternalName(internalName).fqNameForClassNameWithoutDollars

    if (mapBuiltIns) {
        val mappedName = JavaToKotlinClassMap.mapJavaToKotlin(jvmName)
        if (mappedName != null) {
            moduleDescriptor.findClassAcrossModuleDependencies(mappedName)?.let { return it }
        }
    }

    return runReadAction {
        val classes = JavaPsiFacade.getInstance(scope.project).findClasses(jvmName.asString(), scope)
        if (classes.isEmpty()) null
        else {
            classes.first().getJavaClassDescriptor()
        }
    }
}