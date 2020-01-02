class C {
    val plainField: Int = 1
    @delegate:Transient
    val lazy by lazy { 1 }
}