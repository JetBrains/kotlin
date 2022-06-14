/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin

/**
 * The common abstract class of external enum classes.
 */
public abstract external class ExternalEnum {
    public override final fun equals(other: Any?): Boolean
    public override final fun hashCode(): Int
    public override fun toString(): String
}