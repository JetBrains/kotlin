import kotlin.reflect.KClass

annotation class Ann(val arg: Array<out KClass<out Class<*>>>)

// method: Ann::arg
// jvm signature:     ()[Ljava/lang/Class;
// generic signature: ()[Ljava/lang/Class<+Ljava/lang/Class<*>;>;
