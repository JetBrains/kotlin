val prop = @Anno("object annotation") object : @SuperType("super type") List<@SuperType("nested super type") Collection<@SuperType("nested nested super type")Int>>() {
    @Anno("init") init {
        @Expression("expr") foo().let { it: @Anno("lambda param type") Int ->

        }
    }
}
