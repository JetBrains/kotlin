// Please make sure that this test is consistent with the blackbox test "annotationsOnLateinitAccessors.kt"

import kotlin.reflect.KProperty

annotation class Ann
annotation class AnnRepeat

class LateinitProperties {
    @get:Ann
    lateinit var y0: String

    <!ANNOTATION_TARGETS_NON_EXISTENT_ACCESSOR!>@get:Ann<!>
    private lateinit var y1: String
}
