/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.asJava.classes

import com.intellij.openapi.util.Key
import com.intellij.openapi.util.UserDataHolderBase
import org.jetbrains.kotlin.psi.KtClassOrObject

// light class for top level or (inner/nested of top level) source declarations
class KtLightClassImpl(classOrObject: KtClassOrObject) : KtLightClassForSourceDeclaration(classOrObject) {
    override fun getQualifiedName() = classOrObject.fqName?.asString()

    override fun getParent() = if (classOrObject.isTopLevel())
        containingFile
    else
        containingClass

    override fun copy() = KtLightClassImpl(classOrObject.copy() as KtClassOrObject)

    private val sharedDataHolder: UserDataHolderBase
        get() = classOrObject.getUserData(LIGHT_CLASS_SHARED_DATA_HOLDER) ?: synchronized(classOrObject) {
            classOrObject.getUserData(LIGHT_CLASS_SHARED_DATA_HOLDER)?.let { return@synchronized it }
            val holder = UserDataHolderBase()
            classOrObject.putUserData(LIGHT_CLASS_SHARED_DATA_HOLDER, holder)
            holder
        }

    override fun <T : Any?> putUserData(key: Key<T>, value: T?) = sharedDataHolder.putUserData(key, value)

    override fun <T : Any?> getUserData(key: Key<T>): T? = sharedDataHolder.getUserData(key)

    override fun <T : Any?> putUserDataIfAbsent(key: Key<T>, value: T) = sharedDataHolder.putUserDataIfAbsent(key, value)

    override fun <T : Any?> replace(key: Key<T>, oldValue: T?, newValue: T?): Boolean = sharedDataHolder.replace(key, oldValue, newValue)

    override fun <T : Any?> putCopyableUserData(key: Key<T>, value: T?) = sharedDataHolder.putCopyableUserData(key, value)

    override fun getUserDataString(): String = sharedDataHolder.getUserDataString()

    override fun copyCopyableDataTo(clone: UserDataHolderBase) = sharedDataHolder.copyCopyableDataTo(clone)

    override fun copyUserDataTo(other: UserDataHolderBase) = sharedDataHolder.copyUserDataTo(other)

    override fun <T : Any?> getCopyableUserData(key: Key<T>): T? = sharedDataHolder.getCopyableUserData(key)

    override fun isUserDataEmpty(): Boolean = sharedDataHolder.isUserDataEmpty()

}

private val LIGHT_CLASS_SHARED_DATA_HOLDER = Key.create<UserDataHolderBase>("LIGHT_CLASS_SHARED_DATA_HOLDER")
