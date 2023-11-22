@EnumAnnotation("enum")
enum class MyEnumClass {
    @Anno("implicit") @property:Anno("explicit") ENUM_ENTRY {
        @Anno("init annotations") init {
            @Expression("expression annotation") foo()
        }

        @FunAnno("fun")
        fun foo(): @Anno("return type") A<@Anno("nested return type") B> {

        }

        @ErrorPlace("destructuring declaration")
        val (@A("a") a, @B("b") b) = 1 to 2
    }
}
