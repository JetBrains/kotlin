/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.scopes

import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.DescriptorWithDeprecation
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.name.Name

class DeprecatedLexicalScope(private val workerScope: LexicalScope) : LexicalScope by workerScope {
    override fun getContributedClassifiers(name: Name, location: LookupLocation): List<ClassifierDescriptor> = emptyList()

    override fun getContributedClassifiersIncludeDeprecated(
        name: Name,
        location: LookupLocation
    ): List<DescriptorWithDeprecation<ClassifierDescriptor>> {
        return workerScope.getContributedClassifiers(name, location).map { DescriptorWithDeprecation.createDeprecated(it) }
    }
}

class DeprecatedMemberScope(private val workerScope: MemberScope) : MemberScope by workerScope {
    override fun getContributedClassifiers(name: Name, location: LookupLocation): List<ClassifierDescriptor> = emptyList()

    override fun getContributedClassifiersIncludeDeprecated(
        name: Name,
        location: LookupLocation
    ): List<DescriptorWithDeprecation<ClassifierDescriptor>> {
        return workerScope.getContributedClassifiers(name, location).map { DescriptorWithDeprecation.createDeprecated(it) }
    }
}