/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.providers

/**
 * A property annotated with [KaCachedService] stores a cached IntelliJ project or application service.
 *
 * Retrieving services via [Project.getService][com.intellij.openapi.project.Project.getService] and
 * [Application.getService][com.intellij.openapi.application.Application.getService] can have an impact on performance when called
 * frequently. While caching a service is generally not recommended, it can be beneficial for performance in hot spots, if done right.
 *
 * The recommendation to avoid caching services is due to the risk of leaks. Properties annotated with [KaCachedService] should make sure
 * that they don't leak the service: the lifetime of the property should be shorter or as long as the lifetime of the cached service.
 *
 * When caching a service inside another service, the property should also be [lazy] to avoid issues with cyclic service initialization and
 * excessive class loading on startup.
 *
 * The [KaCachedService] annotation itself is currently not enforced by any checkers or inspections, but it serves as an anchor for
 * documentation and improves discoverability of cached services.
 */
@Target(allowedTargets = [AnnotationTarget.PROPERTY, AnnotationTarget.FIELD])
public annotation class KaCachedService
