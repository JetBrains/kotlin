import kotlin.reflect.KClass

annotation class Ann(val arg: Class<*>)

// method: Ann::arg
// jvm signature:     ()Ljava/lang/Class;
// generic signature: ()Ljava/lang/Class<*>;
