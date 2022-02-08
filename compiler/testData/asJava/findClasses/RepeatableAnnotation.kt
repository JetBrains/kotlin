@Repeatable
annotation class RepeatableAnnotation(val value: Int)

@Repeatable
@JvmRepeatable(RepeatableAnnotation2Container::class)
annotation class RepeatableAnnotation2(val value: Int)
annotation class RepeatableAnnotation2Container(val value: Array<RepeatableAnnotation2>)

@JvmRepeatable(RepeatableAnnotation3Container::class)
annotation class RepeatableAnnotation3(val value: Int)
annotation class RepeatableAnnotation3Container(val value: Array<RepeatableAnnotation3>)