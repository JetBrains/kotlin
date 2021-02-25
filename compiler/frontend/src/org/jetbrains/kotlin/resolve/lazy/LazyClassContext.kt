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

package org.jetbrains.kotlin.resolve.lazy

import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.SupertypeLoopChecker
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.extensions.SyntheticResolveExtension
import org.jetbrains.kotlin.resolve.lazy.declarations.DeclarationProviderFactory
import org.jetbrains.kotlin.resolve.sam.SamConversionResolver
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.types.WrappedTypeFactory
import org.jetbrains.kotlin.types.checker.NewKotlinTypeChecker

interface LazyClassContext {
    val declarationScopeProvider: DeclarationScopeProvider

    val storageManager: StorageManager
    val trace: BindingTrace
    val moduleDescriptor: ModuleDescriptor
    val descriptorResolver: DescriptorResolver
    val functionDescriptorResolver: FunctionDescriptorResolver
    val typeResolver: TypeResolver
    val declarationProviderFactory: DeclarationProviderFactory
    val annotationResolver: AnnotationResolver
    val lookupTracker: LookupTracker
    val supertypeLoopChecker: SupertypeLoopChecker
    val languageVersionSettings: LanguageVersionSettings
    val syntheticResolveExtension: SyntheticResolveExtension
    val delegationFilter: DelegationFilter
    val wrappedTypeFactory: WrappedTypeFactory
    val samConversionResolver: SamConversionResolver
    val additionalClassPartsProvider: AdditionalClassPartsProvider
    val sealedClassInheritorsProvider: SealedClassInheritorsProvider

    /**
     * Important notice!
     *
     * This is the type checker of *owner* module, i.e. the module which has declared the class.
     *
     * In MPP, we have different "views" on the one and the same class. I.e. if we have a common 'class Foo : Expect',
     * and on JVM we have 'actual typealias Expect = CharSequence', then from "common view" Foo isn't assigneable to CharSequence,
     * while from the "JVM view" it is.
     *
     * LazyClassContext is usually shared across all such views, so be careful - using this typechecker in "platform views" is not correct.
     * Relevant entities usually have a separate instance of KotlinTypeChecker/KotlinTypeRefiner, see for example
     * [org.jetbrains.kotlin.resolve.lazy.descriptors.LazyClassMemberScope].
     *
     * See also KT-44898 for example of issues which might happen if the wrong typechecker is used in "platform view".
     */
    val kotlinTypeCheckerOfOwnerModule: NewKotlinTypeChecker
}
