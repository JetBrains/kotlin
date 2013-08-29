package test

annotation class Anno

class Class {
    Anno val x: Int by object {
        fun get(thiz: Class, data: PropertyMetadata) = null!!
    }
}
