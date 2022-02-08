// KT-2286 Improve error message for nullability check failure for extension methods

package n

abstract class Buggy {

    abstract val coll : Collection<Int>

    fun getThree(): Int? {
        return coll.find{ it > 3 }  // works fine
    }

    val anotherThree : Int
        get() = <!TYPE_MISMATCH!>coll.find{ it > 3 }<!> // does not work here

    val yetAnotherThree : Int
        get() = <!TYPE_MISMATCH!>coll.find({ v:Int -> v > 3 })<!> // neither here

    val extendedGetter : Int
        get() {
            return <!TYPE_MISMATCH!>coll.find{ it > 3 }<!>  // not even here!
        }

}

//from library
fun <T: Any> Iterable<T>.find(predicate: (T) -> Boolean) : T? {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
