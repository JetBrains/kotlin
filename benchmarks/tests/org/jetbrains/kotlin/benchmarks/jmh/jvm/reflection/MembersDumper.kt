/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.benchmarks.jmh.jvm.reflection

/**
 * Touches `KClass.members` on behalf of [KClassMembersHierarchyBenchmark].
 *
 * This class is loaded again in a class loader whose parent supplies the locally built stdlib and
 * kotlin-reflect, so the `members` call below resolves against that implementation. That is also why the
 * benchmark can only call it reflectively: the two copies of `KClass` come from different class loaders
 * and are unrelated types.
 *
 * Keep every parameter and return type a JDK type - they cross the class loader boundary.
 */
class MembersDumper {
    fun dumpMembers(classLoader: ClassLoader, className: String): String =
        Class.forName(className, true, classLoader).kotlin.members.joinToString { it.toString() }

    fun describeMember(classLoader: ClassLoader, className: String, memberName: String): String {
        val member = Class.forName(className, true, classLoader).kotlin.members.find { it.name == memberName }
            ?: return "<not found>"
        return member::class.java.name
    }
}
