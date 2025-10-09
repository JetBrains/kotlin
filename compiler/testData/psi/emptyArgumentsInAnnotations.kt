// COMPILATION_ERRORS

@Ann(1, ,)
@Ann(,)
@Ann("", , "")
@Ann(, [])
@Ann(, 2,)
fun simple() { }

@Ann(x =, )
@Ann(x =)
@Ann(1, x =)
@Ann(x = , [])
@Ann(x = , y =)
@Ann(x = , y = [])
fun named() { }

@Ann(*,)
@Ann(1, *)
@Ann(*, 2)
fun spread() { }

@Ann(x = *)
@Ann(x = *,)
@Ann(, *)
@Ann(x = ,*)
@Ann(*, y =)
fun mixed() { }