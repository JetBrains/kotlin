fun getEnumToClassFoo(): EnumToClass = EnumToClass.Foo
inline fun getEnumToClassFooInline(): EnumToClass = EnumToClass.Foo
fun getEnumToClassFooAsAny(): Any = EnumToClass.Foo
inline fun getEnumToClassFooAsAnyInline(): Any = EnumToClass.Foo

fun getEnumToClassBar(): EnumToClass = EnumToClass.Bar
inline fun getEnumToClassBarInline(): EnumToClass = EnumToClass.Bar
fun getEnumToClassBarAsAny(): Any = EnumToClass.Bar
inline fun getEnumToClassBarAsAnyInline(): Any = EnumToClass.Bar

fun getObjectToEnumFoo(): ObjectToEnum.Foo = ObjectToEnum.Foo()
inline fun getObjectToEnumFooInline(): ObjectToEnum.Foo = ObjectToEnum.Foo()
fun getObjectToEnumFooAsAny(): Any = ObjectToEnum.Foo()
inline fun getObjectToEnumFooAsAnyInline(): Any = ObjectToEnum.Foo()

fun getObjectToEnumBar(): ObjectToEnum.Bar = ObjectToEnum.Bar
inline fun getObjectToEnumBarInline(): ObjectToEnum.Bar = ObjectToEnum.Bar
fun getObjectToEnumBarAsAny(): Any = ObjectToEnum.Bar
inline fun getObjectToEnumBarAsAnyInline(): Any = ObjectToEnum.Bar

fun getClassToEnumFoo(): ClassToEnum.Foo = ClassToEnum.Foo()
inline fun getClassToEnumFooInline(): ClassToEnum.Foo = ClassToEnum.Foo()
fun getClassToEnumFooAsAny(): Any = ClassToEnum.Foo()
inline fun getClassToEnumFooAsAnyInline(): Any = ClassToEnum.Foo()

fun getClassToEnumBar(): ClassToEnum.Bar = ClassToEnum.Bar
inline fun getClassToEnumBarInline(): ClassToEnum.Bar = ClassToEnum.Bar
fun getClassToEnumBarAsAny(): Any = ClassToEnum.Bar
inline fun getClassToEnumBarAsAnyInline(): Any = ClassToEnum.Bar

fun getClassToEnumBaz(): ClassToEnum.Baz = ClassToEnum().Baz()
inline fun getClassToEnumBazInline(): ClassToEnum.Baz = ClassToEnum().Baz()
fun getClassToEnumBazAsAny(): Any = ClassToEnum().Baz()
inline fun getClassToEnumBazAsAnyInline(): Any = ClassToEnum().Baz()

fun getClassToObject(): ClassToObject = ClassToObject()
inline fun getClassToObjectInline(): ClassToObject = ClassToObject()
fun getClassToObjectAsAny(): Any = ClassToObject()
inline fun getClassToObjectAsAnyInline(): Any = ClassToObject()

fun getObjectToClass(): ObjectToClass = ObjectToClass
inline fun getObjectToClassInline(): ObjectToClass = ObjectToClass
fun getObjectToClassAsAny(): Any = ObjectToClass
inline fun getObjectToClassAsAnyInline(): Any = ObjectToClass
