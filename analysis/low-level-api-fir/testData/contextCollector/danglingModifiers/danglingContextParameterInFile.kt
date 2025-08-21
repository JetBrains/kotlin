@Target(AnnotationTarget.TYPE)
annotation class Anno(val position: String)

context(<expr>parameter1 : @Anno("1" + "2") String</expr>, parameter2: List<@Anno("str") Int>)
