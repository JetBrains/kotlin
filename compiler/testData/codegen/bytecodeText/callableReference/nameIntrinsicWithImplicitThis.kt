class Foo {
    fun bar(): String {
        return ::bar.name
    }
}

// We can avoid loading (and then immediately popping) implicit "this" when generating intrinsified bytecode for KCallable.name
// because this can't have any side effects
// 0 POP
