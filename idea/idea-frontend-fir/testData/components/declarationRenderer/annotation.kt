@Target(AnnotationTarget.ANNOTATION_CLASS) annotation class base

@base annotation class derived

@base class correct(@base val x: Int) {
    @base constructor(): this(0)
}

@base enum class My {
    @base FIRST,
    @base SECOND
}

@base fun foo(@base y: @base Int): Int {
    @base fun bar(@base z: @base Int) = z + 1
    @base val local = bar(y)
    return local
}

@base val z = 0
