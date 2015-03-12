/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.load.java.structure.reflect

import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.JavaPackage
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

public class ReflectJavaPackage(private val fqName: FqName) : ReflectJavaElement(), JavaPackage {
    override fun getFqName() = fqName

    override fun getClasses(nameFilter: (Name) -> Boolean): Collection<JavaClass> {
        // A package at runtime can't know what classes it has and has not
        return listOf()
    }

    override fun getSubPackages(): Collection<JavaPackage> {
        // A package at runtime can't know what sub packages it has and has not
        return listOf()
    }

    override fun equals(other: Any?) = other is ReflectJavaPackage && fqName == other.fqName

    override fun hashCode() = fqName.hashCode()

    override fun toString() = javaClass.getName() + ": " + fqName
}
