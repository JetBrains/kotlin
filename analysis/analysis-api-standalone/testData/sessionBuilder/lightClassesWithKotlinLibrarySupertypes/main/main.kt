package org.test

import lib.LibraryClass
import lib.LibraryInterface

class ItemList<T> : AbstractList<T?>() {
    override fun get(index: Int): T? = null
    override val size: Int get() = 0
}

class LibrarySubclass : LibraryClass(), LibraryInterface
