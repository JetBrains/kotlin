import kotlin.reflect.KClass

annotation class Ann(val arg: KClass<*>)

// method: Ann::arg
// jvm signature:     ()Ljava/lang/Class;
// generic signature: ()Ljava/lang/Class<*>;
