fun foo(c : Collection<String>){
  c.filter<selection>{it; false}</selection>
}
/*
fun foo(c : Collection<String>){
    val function = {it; false}
    c.filter(function)
}
*/