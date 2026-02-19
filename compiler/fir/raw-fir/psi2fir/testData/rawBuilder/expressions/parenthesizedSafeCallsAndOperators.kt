// ISSUE: KT-68834

fun foo() {
    arg?.alias[42] 
    arg?.alias[42] = "" 
    arg?.alias += "" 
    arg?.alias++ 
    ++arg?.alias 
    arg?.alias("") 
    arg?.alias[42] += ""
    arg?.alias[42]++
    ++arg?.alias[42]

    (arg?.alias)[42] 
    (arg?.alias)[42] = "" 
    (arg?.alias) += "" 
    (arg?.alias)++ 
    ++(arg?.alias) 
    (arg?.alias)("") 
    (arg?.alias)[42] += ""
    (arg?.alias[42]) += ""
    (arg?.alias[42])++
    ++(arg?.alias[42])
}
