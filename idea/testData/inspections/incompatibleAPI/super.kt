package problem.api.kotlin.s

import lib.LibSuper

class Subclass : LibSuper() {
    override fun test(str: String?) {
    }
}

class SubclassSuppress : LibSuper() {
    @Suppress("IncompatibleAPI")
    override fun test(str: String?) {
    }
}