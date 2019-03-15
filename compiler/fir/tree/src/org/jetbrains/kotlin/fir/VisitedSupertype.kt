/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

// This annotation is used in FIR interface super-type list
// It *should* be used if super-type list includes more than one FIR element
// It marks the single super-type to which visitor should proceed by default

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.TYPE)
annotation class VisitedSupertype