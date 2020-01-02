// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -NOTHING_TO_INLINE

public class Z {
    private val privateProperty = 11;

    public fun privateFun() {

        class Local {
            public inline fun a() {
                privateProperty
            }
        }

    }
}