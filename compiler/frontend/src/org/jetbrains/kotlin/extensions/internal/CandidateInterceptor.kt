/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.extensions.internal

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.extensions.ProjectExtensionDescriptor
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.calls.CallResolver
import org.jetbrains.kotlin.resolve.calls.CandidateResolver
import org.jetbrains.kotlin.resolve.calls.context.BasicCallResolutionContext
import org.jetbrains.kotlin.resolve.calls.tasks.TracingStrategy
import org.jetbrains.kotlin.resolve.calls.tower.ImplicitScopeTower
import org.jetbrains.kotlin.resolve.calls.tower.NewResolutionOldInference
import org.jetbrains.kotlin.resolve.scopes.ResolutionScope

@UseExperimental(InternalNonStableExtensionPoints::class)
class CandidateInterceptor(project: Project) {
    private val extensions = getInstances(project)

    fun interceptResolvedCandidates(
        candidates: Collection<NewResolutionOldInference.MyCandidate>,
        context: BasicCallResolutionContext,
        candidateResolver: CandidateResolver,
        callResolver: CallResolver?,
        name: Name,
        kind: NewResolutionOldInference.ResolutionKind,
        tracing: TracingStrategy
    ) = extensions.fold(candidates) { it, extension ->
        extension.interceptCandidates(it, context, candidateResolver, callResolver, name, kind, tracing)
    }

    fun interceptCandidates(
        candidates: Collection<FunctionDescriptor>,
        scopeTower: ImplicitScopeTower,
        resolutionContext: BasicCallResolutionContext,
        resolutionScope: ResolutionScope,
        callResolver: CallResolver?,
        name: Name,
        location: LookupLocation
    ): Collection<FunctionDescriptor> = extensions.fold(candidates) { it, extension ->
        extension.interceptCandidates(it, scopeTower, resolutionContext, resolutionScope, callResolver, name, location)
    }

    companion object : ProjectExtensionDescriptor<CallResolutionInterceptorExtension>(
        "org.jetbrains.kotlin.extensions.internal.callResolutionInterceptorExtension",
        CallResolutionInterceptorExtension::class.java
    )
}