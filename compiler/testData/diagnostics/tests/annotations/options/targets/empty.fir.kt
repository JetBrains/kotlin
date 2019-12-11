// !DIAGNOSTICS: -UNUSED_PARAMETER
@Target() annotation class empty

@empty annotation class derived

@empty class correct(@empty val x: Int, @empty w: @empty Int) {
    @empty constructor(): this(0, 0)
}

@empty enum class My @empty constructor() {
    @empty FIRST,
    @empty SECOND
}

@empty fun foo(@empty y: @empty Int): Int {
    @empty fun bar(@empty z: @empty Int) = z + 1
    @empty val local = bar(y)
    return local
}

@empty val z = @empty 0
