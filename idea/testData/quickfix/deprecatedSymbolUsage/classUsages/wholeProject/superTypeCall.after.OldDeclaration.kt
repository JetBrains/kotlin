package pack

@Deprecated("Replace with NewClass", ReplaceWith("NewClass({ i })", "newPack.NewClass"))
open class OldClass(val i: Int)