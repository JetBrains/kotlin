import kotlin.wasm.WasmImport

/*
@WitInterface("hi")
external interface Hiiiimport{
    // this breaks linking in case Hiiiimport is *not* marked external
    // makes sense, because it tries to call Import.foo, but that's not defined here
    // -> for now, transform the external interface to a normal one in the ir transform
    @WitImport
    companion object Import : Hiiiimport

    fun foo(): String
}

@WitInterface("hi")
external interface Heeeexport{
    fun foo(): String
}

// TODO this breaks binary/text emission in case Heeeexport is marked external
@WitExport
object HeeeexportImpl : Heeeexport{
    override fun foo() = "OK"
}

fun toplevel(){
}


 */

/*
//@WasmImport("abc", "queryForPersonTopLevel")
//external fun queryForPersonTopLevel(query: Database.MyInefficientString): Database.Person

@WitInterface(/*TODO*/"example:use-types/database@1.0.0")
external interface Database {
  @WitImport/*(TODO)*/
  companion object Import : Database/* by stdlib.witMagicIntrinsic()*/

  typealias MyInefficientString = List<UByte>

  typealias Person = MyTypes.Person
  fun queryForPerson(query: Database.MyInefficientString): Database.Person
}
@WitInterface(/*TODO*/"example:use-types/user-facing-server@1.0.0")
external interface UserFacingServer {

  typealias Person = MyTypes.Person
  fun serveWebsiteWithUsers(users: List<UserFacingServer.Person>)
}
@WitInterface(/*TODO*/"example:use-types/my-types@1.0.0")
external interface MyTypes {
  @WitImport/*(TODO)*/
  companion object Import : MyTypes/* by stdlib.witMagicIntrinsic()*/

  data class Person(
  var name: List<UByte>,
  var age: UInt,
  )
}



 */

/*
example for an external module:

(module $rust_to_wasm.wasm
 (type $none_=>_i32 (func (result i32)))
 (global $__stack_pointer (mut i32) (i32.const 1048576))
 (global $global$1 i32 (i32.const 1048576))
 (global $global$2 i32 (i32.const 1048576))
 (memory $0 16)
 (export "memory" (memory $0))
 (export "foo" (func $foo))
 (export "__data_end" (global $global$1))
 (export "__heap_base" (global $global$2))
 (func $foo (result i32)
  (i32.const 42)
 )
 ;; custom section "producers", size 67
 ;; features section: mutable-globals, nontrapping-float-to-int, bulk-memory, sign-ext, reference-types, multivalue
)
 */


@WitInterface("example:trivial-import/im-imported@1.0.0")
external interface ImImported {
  @WitImport
  companion object Import : ImImported
  fun foo(): Int
}

//@WasmImport("example:trivial-import/im-imported@1.0.0", "foo")
//external fun foo() : Int


fun box():String {
  return "OK"
//    return Hiiiimport.Import.foo()
//    return HeeeexportImpl.foo()
//    return "OK"
}
