interface ReadWriteProperty<R, T> {
    operator fun getValue(thisRef: R, prop: Any): T
    operator fun setValue(thisRef: R, prop: Any, value: T)
}


interface Delegate<R, T> : ReadWriteProperty<R, T>

interface DatabaseEntity


fun <Self : DatabaseEntity, Target : DatabaseEntity> Self.directed(clazz: Class<Target>):
        Delegate<Self, Target?> = null!!

class MyClassSome : DatabaseEntity {
    var other by directed(MyClassSome::class.java)
    fun set(link: MyClassSome?) {
        other = link
    }
}
