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

import java.lang.reflect.Member
import java.lang.reflect.Modifier
import org.jetbrains.kotlin.load.java.structure.JavaMember
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames

public abstract class ReflectJavaMember(protected val member: Member) : ReflectJavaElement(), JavaMember {
    override fun getName() = member.getName()?.let { Name.identifier(it) } ?: SpecialNames.NO_NAME_PROVIDED

    override fun getContainingClass() = ReflectJavaClass(member.getDeclaringClass())

    override fun isAbstract() = Modifier.isAbstract(member.getModifiers())
    override fun isStatic() = Modifier.isStatic(member.getModifiers())
    override fun isFinal() = Modifier.isFinal(member.getModifiers())

    override fun getVisibility() = calculateVisibility(member.getModifiers())
}
