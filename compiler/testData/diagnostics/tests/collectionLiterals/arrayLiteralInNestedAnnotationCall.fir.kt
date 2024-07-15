// DIAGNOSTICS: -REDUNDANT_SPREAD_OPERATOR_IN_NAMED_FORM_IN_ANNOTATION

annotation class Anno1In(val x: Array<in Anno2In>)
annotation class Anno2In(val x: Array<in String>)

annotation class Anno1Out(val x: Array<out Anno2Out>)
annotation class Anno2Out(val x: Array<out String>)

@Repeatable
annotation class Anno1Inv(val x: Array<Anno2Inv>)
annotation class Anno2Inv(val x: Array<String>)

@Repeatable
annotation class Anno1Vararg(vararg val x: Anno2Inv)

@Anno1In(x = [Anno2In(x = [1])])
@Anno1Out(x = [Anno2Out(x = [1])])
@Anno1Inv(x = [Anno2Inv(x = [1])])
@Anno1Inv(x = arrayOf(Anno2Inv(x = [1])))
@Anno1Vararg(x = [Anno2Inv(x = [1])])
@Anno1Vararg(Anno2Inv(x = [1]))
@Anno1Vararg(x = *[Anno2Inv(x = [1])])
@Anno1Vararg(x = *arrayOf(Anno2Inv(x = [1])))
fun foo() {}
