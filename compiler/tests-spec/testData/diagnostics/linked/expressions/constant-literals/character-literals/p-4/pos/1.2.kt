// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-218
 * PLACE: expressions, constant-literals, character-literals -> paragraph 4 -> sentence 1
 * RELEVANT PLACES: expressions, constant-literals, character-literals -> paragraph 5 -> sentence 1
 * expressions, constant-literals, character-literals -> paragraph 6 -> sentence 1
 * NUMBER: 2
 * DESCRIPTION: to define a character the unicode code point escaped symbol \u could be used with followed by exactly four hexadecimal digits
 * HELPERS: checkType
 */

// TESTCASE NUMBER: 1
fun case1(){
   val case1 = '\u0000'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case1<!>
   case1 checkType { check<Char>()}
}
// TESTCASE NUMBER: 2
fun case2(){
   val case2 = '\u00ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case2<!>
   case2 checkType { check<Char>()}
}
// TESTCASE NUMBER: 3
fun case3(){
   val case3 = '\u0100'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case3<!>
   case3 checkType { check<Char>()}
}
// TESTCASE NUMBER: 4
fun case4(){
   val case4 = '\u01ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case4<!>
   case4 checkType { check<Char>()}
}
// TESTCASE NUMBER: 5
fun case5(){
   val case5 = '\u0200'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case5<!>
   case5 checkType { check<Char>()}
}
// TESTCASE NUMBER: 6
fun case6(){
   val case6 = '\u02ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case6<!>
   case6 checkType { check<Char>()}
}
// TESTCASE NUMBER: 7
fun case7(){
   val case7 = '\u0300'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case7<!>
   case7 checkType { check<Char>()}
}
// TESTCASE NUMBER: 8
fun case8(){
   val case8 = '\u03ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case8<!>
   case8 checkType { check<Char>()}
}
// TESTCASE NUMBER: 9
fun case9(){
   val case9 = '\u0400'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case9<!>
   case9 checkType { check<Char>()}
}
// TESTCASE NUMBER: 10
fun case10(){
   val case10 = '\u04ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case10<!>
   case10 checkType { check<Char>()}
}
// TESTCASE NUMBER: 11
fun case11(){
   val case11 = '\u0500'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case11<!>
   case11 checkType { check<Char>()}
}
// TESTCASE NUMBER: 12
fun case12(){
   val case12 = '\u05ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case12<!>
   case12 checkType { check<Char>()}
}
// TESTCASE NUMBER: 13
fun case13(){
   val case13 = '\u0600'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case13<!>
   case13 checkType { check<Char>()}
}
// TESTCASE NUMBER: 14
fun case14(){
   val case14 = '\u06ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case14<!>
   case14 checkType { check<Char>()}
}
// TESTCASE NUMBER: 15
fun case15(){
   val case15 = '\u0700'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case15<!>
   case15 checkType { check<Char>()}
}
// TESTCASE NUMBER: 16
fun case16(){
   val case16 = '\u07ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case16<!>
   case16 checkType { check<Char>()}
}
// TESTCASE NUMBER: 17
fun case17(){
   val case17 = '\u0800'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case17<!>
   case17 checkType { check<Char>()}
}
// TESTCASE NUMBER: 18
fun case18(){
   val case18 = '\u08ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case18<!>
   case18 checkType { check<Char>()}
}
// TESTCASE NUMBER: 19
fun case19(){
   val case19 = '\u0900'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case19<!>
   case19 checkType { check<Char>()}
}
// TESTCASE NUMBER: 20
fun case20(){
   val case20 = '\u09ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case20<!>
   case20 checkType { check<Char>()}
}
// TESTCASE NUMBER: 21
fun case21(){
   val case21 = '\u0a00'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case21<!>
   case21 checkType { check<Char>()}
}
// TESTCASE NUMBER: 22
fun case22(){
   val case22 = '\u0aff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case22<!>
   case22 checkType { check<Char>()}
}
// TESTCASE NUMBER: 23
fun case23(){
   val case23 = '\u0b00'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case23<!>
   case23 checkType { check<Char>()}
}
// TESTCASE NUMBER: 24
fun case24(){
   val case24 = '\u0bff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case24<!>
   case24 checkType { check<Char>()}
}
// TESTCASE NUMBER: 25
fun case25(){
   val case25 = '\u0c00'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case25<!>
   case25 checkType { check<Char>()}
}
// TESTCASE NUMBER: 26
fun case26(){
   val case26 = '\u0cff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case26<!>
   case26 checkType { check<Char>()}
}
// TESTCASE NUMBER: 27
fun case27(){
   val case27 = '\u0d00'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case27<!>
   case27 checkType { check<Char>()}
}
// TESTCASE NUMBER: 28
fun case28(){
   val case28 = '\u0dff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case28<!>
   case28 checkType { check<Char>()}
}
// TESTCASE NUMBER: 29
fun case29(){
   val case29 = '\u0e00'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case29<!>
   case29 checkType { check<Char>()}
}
// TESTCASE NUMBER: 30
fun case30(){
   val case30 = '\u0eff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case30<!>
   case30 checkType { check<Char>()}
}
// TESTCASE NUMBER: 31
fun case31(){
   val case31 = '\u0f00'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case31<!>
   case31 checkType { check<Char>()}
}
// TESTCASE NUMBER: 32
fun case32(){
   val case32 = '\u0fff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case32<!>
   case32 checkType { check<Char>()}
}
// TESTCASE NUMBER: 33
fun case33(){
   val case33 = '\u1000'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case33<!>
   case33 checkType { check<Char>()}
}
// TESTCASE NUMBER: 34
fun case34(){
   val case34 = '\u10ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case34<!>
   case34 checkType { check<Char>()}
}
// TESTCASE NUMBER: 35
fun case35(){
   val case35 = '\u1100'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case35<!>
   case35 checkType { check<Char>()}
}
// TESTCASE NUMBER: 36
fun case36(){
   val case36 = '\u11ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case36<!>
   case36 checkType { check<Char>()}
}
// TESTCASE NUMBER: 37
fun case37(){
   val case37 = '\u1200'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case37<!>
   case37 checkType { check<Char>()}
}
// TESTCASE NUMBER: 38
fun case38(){
   val case38 = '\u12ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case38<!>
   case38 checkType { check<Char>()}
}
// TESTCASE NUMBER: 39
fun case39(){
   val case39 = '\u1300'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case39<!>
   case39 checkType { check<Char>()}
}
// TESTCASE NUMBER: 40
fun case40(){
   val case40 = '\u13ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case40<!>
   case40 checkType { check<Char>()}
}
// TESTCASE NUMBER: 41
fun case41(){
   val case41 = '\u1400'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case41<!>
   case41 checkType { check<Char>()}
}
// TESTCASE NUMBER: 42
fun case42(){
   val case42 = '\u14ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case42<!>
   case42 checkType { check<Char>()}
}
// TESTCASE NUMBER: 43
fun case43(){
   val case43 = '\u1500'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case43<!>
   case43 checkType { check<Char>()}
}
// TESTCASE NUMBER: 44
fun case44(){
   val case44 = '\u15ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case44<!>
   case44 checkType { check<Char>()}
}
// TESTCASE NUMBER: 45
fun case45(){
   val case45 = '\u1600'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case45<!>
   case45 checkType { check<Char>()}
}
// TESTCASE NUMBER: 46
fun case46(){
   val case46 = '\u16ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case46<!>
   case46 checkType { check<Char>()}
}
// TESTCASE NUMBER: 47
fun case47(){
   val case47 = '\u1700'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case47<!>
   case47 checkType { check<Char>()}
}
// TESTCASE NUMBER: 48
fun case48(){
   val case48 = '\u17ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case48<!>
   case48 checkType { check<Char>()}
}
// TESTCASE NUMBER: 49
fun case49(){
   val case49 = '\u1800'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case49<!>
   case49 checkType { check<Char>()}
}
// TESTCASE NUMBER: 50
fun case50(){
   val case50 = '\u18ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case50<!>
   case50 checkType { check<Char>()}
}
// TESTCASE NUMBER: 51
fun case51(){
   val case51 = '\u1900'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case51<!>
   case51 checkType { check<Char>()}
}
// TESTCASE NUMBER: 52
fun case52(){
   val case52 = '\u19ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case52<!>
   case52 checkType { check<Char>()}
}
// TESTCASE NUMBER: 53
fun case53(){
   val case53 = '\u1a00'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case53<!>
   case53 checkType { check<Char>()}
}
// TESTCASE NUMBER: 54
fun case54(){
   val case54 = '\u1aff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case54<!>
   case54 checkType { check<Char>()}
}
// TESTCASE NUMBER: 55
fun case55(){
   val case55 = '\u1b00'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case55<!>
   case55 checkType { check<Char>()}
}
// TESTCASE NUMBER: 56
fun case56(){
   val case56 = '\u1bff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case56<!>
   case56 checkType { check<Char>()}
}
// TESTCASE NUMBER: 57
fun case57(){
   val case57 = '\u1c00'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case57<!>
   case57 checkType { check<Char>()}
}
// TESTCASE NUMBER: 58
fun case58(){
   val case58 = '\u1cff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case58<!>
   case58 checkType { check<Char>()}
}
// TESTCASE NUMBER: 59
fun case59(){
   val case59 = '\u1d00'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case59<!>
   case59 checkType { check<Char>()}
}
// TESTCASE NUMBER: 60
fun case60(){
   val case60 = '\u1dff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case60<!>
   case60 checkType { check<Char>()}
}
// TESTCASE NUMBER: 61
fun case61(){
   val case61 = '\u1e00'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case61<!>
   case61 checkType { check<Char>()}
}
// TESTCASE NUMBER: 62
fun case62(){
   val case62 = '\u1eff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case62<!>
   case62 checkType { check<Char>()}
}
// TESTCASE NUMBER: 63
fun case63(){
   val case63 = '\u1f00'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case63<!>
   case63 checkType { check<Char>()}
}
// TESTCASE NUMBER: 64
fun case64(){
   val case64 = '\u1fff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case64<!>
   case64 checkType { check<Char>()}
}
// TESTCASE NUMBER: 65
fun case65(){
   val case65 = '\u2000'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case65<!>
   case65 checkType { check<Char>()}
}
// TESTCASE NUMBER: 66
fun case66(){
   val case66 = '\u20ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case66<!>
   case66 checkType { check<Char>()}
}
// TESTCASE NUMBER: 67
fun case67(){
   val case67 = '\u2100'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case67<!>
   case67 checkType { check<Char>()}
}
// TESTCASE NUMBER: 68
fun case68(){
   val case68 = '\u21ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case68<!>
   case68 checkType { check<Char>()}
}
// TESTCASE NUMBER: 69
fun case69(){
   val case69 = '\u2200'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case69<!>
   case69 checkType { check<Char>()}
}
// TESTCASE NUMBER: 70
fun case70(){
   val case70 = '\u22ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case70<!>
   case70 checkType { check<Char>()}
}
// TESTCASE NUMBER: 71
fun case71(){
   val case71 = '\u2300'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case71<!>
   case71 checkType { check<Char>()}
}
// TESTCASE NUMBER: 72
fun case72(){
   val case72 = '\u23ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case72<!>
   case72 checkType { check<Char>()}
}
// TESTCASE NUMBER: 73
fun case73(){
   val case73 = '\u2400'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case73<!>
   case73 checkType { check<Char>()}
}
// TESTCASE NUMBER: 74
fun case74(){
   val case74 = '\u24ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case74<!>
   case74 checkType { check<Char>()}
}
// TESTCASE NUMBER: 75
fun case75(){
   val case75 = '\u2500'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case75<!>
   case75 checkType { check<Char>()}
}
// TESTCASE NUMBER: 76
fun case76(){
   val case76 = '\u25ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case76<!>
   case76 checkType { check<Char>()}
}
// TESTCASE NUMBER: 77
fun case77(){
   val case77 = '\u2600'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case77<!>
   case77 checkType { check<Char>()}
}
// TESTCASE NUMBER: 78
fun case78(){
   val case78 = '\u26ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case78<!>
   case78 checkType { check<Char>()}
}
// TESTCASE NUMBER: 79
fun case79(){
   val case79 = '\u2700'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case79<!>
   case79 checkType { check<Char>()}
}
// TESTCASE NUMBER: 80
fun case80(){
   val case80 = '\u27ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case80<!>
   case80 checkType { check<Char>()}
}
// TESTCASE NUMBER: 81
fun case81(){
   val case81 = '\u2800'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case81<!>
   case81 checkType { check<Char>()}
}
// TESTCASE NUMBER: 82
fun case82(){
   val case82 = '\u28ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case82<!>
   case82 checkType { check<Char>()}
}
// TESTCASE NUMBER: 83
fun case83(){
   val case83 = '\u2900'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case83<!>
   case83 checkType { check<Char>()}
}
// TESTCASE NUMBER: 84
fun case84(){
   val case84 = '\u29ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case84<!>
   case84 checkType { check<Char>()}
}
// TESTCASE NUMBER: 85
fun case85(){
   val case85 = '\u2a00'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case85<!>
   case85 checkType { check<Char>()}
}
// TESTCASE NUMBER: 86
fun case86(){
   val case86 = '\u2aff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case86<!>
   case86 checkType { check<Char>()}
}
// TESTCASE NUMBER: 87
fun case87(){
   val case87 = '\u2b00'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case87<!>
   case87 checkType { check<Char>()}
}
// TESTCASE NUMBER: 88
fun case88(){
   val case88 = '\u2bff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case88<!>
   case88 checkType { check<Char>()}
}
// TESTCASE NUMBER: 89
fun case89(){
   val case89 = '\u2c00'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case89<!>
   case89 checkType { check<Char>()}
}
// TESTCASE NUMBER: 90
fun case90(){
   val case90 = '\u2cff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case90<!>
   case90 checkType { check<Char>()}
}
// TESTCASE NUMBER: 91
fun case91(){
   val case91 = '\u2d00'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case91<!>
   case91 checkType { check<Char>()}
}
// TESTCASE NUMBER: 92
fun case92(){
   val case92 = '\u2dff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case92<!>
   case92 checkType { check<Char>()}
}
// TESTCASE NUMBER: 93
fun case93(){
   val case93 = '\u2e00'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case93<!>
   case93 checkType { check<Char>()}
}
// TESTCASE NUMBER: 94
fun case94(){
   val case94 = '\u2eff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case94<!>
   case94 checkType { check<Char>()}
}
// TESTCASE NUMBER: 95
fun case95(){
   val case95 = '\u2f00'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case95<!>
   case95 checkType { check<Char>()}
}
// TESTCASE NUMBER: 96
fun case96(){
   val case96 = '\u2fff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case96<!>
   case96 checkType { check<Char>()}
}
// TESTCASE NUMBER: 97
fun case97(){
   val case97 = '\u3000'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case97<!>
   case97 checkType { check<Char>()}
}
// TESTCASE NUMBER: 98
fun case98(){
   val case98 = '\u30ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case98<!>
   case98 checkType { check<Char>()}
}
// TESTCASE NUMBER: 99
fun case99(){
   val case99 = '\u3100'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case99<!>
   case99 checkType { check<Char>()}
}
// TESTCASE NUMBER: 100
fun case100(){
   val case100 = '\u31ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case100<!>
   case100 checkType { check<Char>()}
}
// TESTCASE NUMBER: 101
fun case101(){
   val case101 = '\u3200'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case101<!>
   case101 checkType { check<Char>()}
}
// TESTCASE NUMBER: 102
fun case102(){
   val case102 = '\u32ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case102<!>
   case102 checkType { check<Char>()}
}
// TESTCASE NUMBER: 103
fun case103(){
   val case103 = '\u3300'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case103<!>
   case103 checkType { check<Char>()}
}
// TESTCASE NUMBER: 104
fun case104(){
   val case104 = '\u33ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case104<!>
   case104 checkType { check<Char>()}
}
// TESTCASE NUMBER: 105
fun case105(){
   val case105 = '\u3400'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case105<!>
   case105 checkType { check<Char>()}
}
// TESTCASE NUMBER: 106
fun case106(){
   val case106 = '\u34ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case106<!>
   case106 checkType { check<Char>()}
}
// TESTCASE NUMBER: 107
fun case107(){
   val case107 = '\u3500'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case107<!>
   case107 checkType { check<Char>()}
}
// TESTCASE NUMBER: 108
fun case108(){
   val case108 = '\u35ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case108<!>
   case108 checkType { check<Char>()}
}
// TESTCASE NUMBER: 109
fun case109(){
   val case109 = '\u3600'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case109<!>
   case109 checkType { check<Char>()}
}
// TESTCASE NUMBER: 110
fun case110(){
   val case110 = '\u36ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case110<!>
   case110 checkType { check<Char>()}
}
// TESTCASE NUMBER: 111
fun case111(){
   val case111 = '\u3700'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case111<!>
   case111 checkType { check<Char>()}
}
// TESTCASE NUMBER: 112
fun case112(){
   val case112 = '\u37ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case112<!>
   case112 checkType { check<Char>()}
}
// TESTCASE NUMBER: 113
fun case113(){
   val case113 = '\u3800'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case113<!>
   case113 checkType { check<Char>()}
}
// TESTCASE NUMBER: 114
fun case114(){
   val case114 = '\u38ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case114<!>
   case114 checkType { check<Char>()}
}
// TESTCASE NUMBER: 115
fun case115(){
   val case115 = '\u3900'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case115<!>
   case115 checkType { check<Char>()}
}
// TESTCASE NUMBER: 116
fun case116(){
   val case116 = '\u39ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case116<!>
   case116 checkType { check<Char>()}
}
// TESTCASE NUMBER: 117
fun case117(){
   val case117 = '\u3a00'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case117<!>
   case117 checkType { check<Char>()}
}
// TESTCASE NUMBER: 118
fun case118(){
   val case118 = '\u3aff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case118<!>
   case118 checkType { check<Char>()}
}
// TESTCASE NUMBER: 119
fun case119(){
   val case119 = '\u3b00'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case119<!>
   case119 checkType { check<Char>()}
}
// TESTCASE NUMBER: 120
fun case120(){
   val case120 = '\u3bff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case120<!>
   case120 checkType { check<Char>()}
}
// TESTCASE NUMBER: 121
fun case121(){
   val case121 = '\u3c00'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case121<!>
   case121 checkType { check<Char>()}
}
// TESTCASE NUMBER: 122
fun case122(){
   val case122 = '\u3cff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case122<!>
   case122 checkType { check<Char>()}
}
// TESTCASE NUMBER: 123
fun case123(){
   val case123 = '\u3d00'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case123<!>
   case123 checkType { check<Char>()}
}
// TESTCASE NUMBER: 124
fun case124(){
   val case124 = '\u3dff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case124<!>
   case124 checkType { check<Char>()}
}
// TESTCASE NUMBER: 125
fun case125(){
   val case125 = '\u3e00'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case125<!>
   case125 checkType { check<Char>()}
}
// TESTCASE NUMBER: 126
fun case126(){
   val case126 = '\u3eff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case126<!>
   case126 checkType { check<Char>()}
}
// TESTCASE NUMBER: 127
fun case127(){
   val case127 = '\u3f00'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case127<!>
   case127 checkType { check<Char>()}
}
// TESTCASE NUMBER: 128
fun case128(){
   val case128 = '\u3fff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case128<!>
   case128 checkType { check<Char>()}
}
// TESTCASE NUMBER: 129
fun case129(){
   val case129 = '\u4000'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case129<!>
   case129 checkType { check<Char>()}
}
// TESTCASE NUMBER: 130
fun case130(){
   val case130 = '\u40ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case130<!>
   case130 checkType { check<Char>()}
}
// TESTCASE NUMBER: 131
fun case131(){
   val case131 = '\u4100'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case131<!>
   case131 checkType { check<Char>()}
}
// TESTCASE NUMBER: 132
fun case132(){
   val case132 = '\u41ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case132<!>
   case132 checkType { check<Char>()}
}
// TESTCASE NUMBER: 133
fun case133(){
   val case133 = '\u4200'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case133<!>
   case133 checkType { check<Char>()}
}
// TESTCASE NUMBER: 134
fun case134(){
   val case134 = '\u42ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case134<!>
   case134 checkType { check<Char>()}
}
// TESTCASE NUMBER: 135
fun case135(){
   val case135 = '\u4300'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case135<!>
   case135 checkType { check<Char>()}
}
// TESTCASE NUMBER: 136
fun case136(){
   val case136 = '\u43ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case136<!>
   case136 checkType { check<Char>()}
}
// TESTCASE NUMBER: 137
fun case137(){
   val case137 = '\u4400'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case137<!>
   case137 checkType { check<Char>()}
}
// TESTCASE NUMBER: 138
fun case138(){
   val case138 = '\u44ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case138<!>
   case138 checkType { check<Char>()}
}
// TESTCASE NUMBER: 139
fun case139(){
   val case139 = '\u4500'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case139<!>
   case139 checkType { check<Char>()}
}
// TESTCASE NUMBER: 140
fun case140(){
   val case140 = '\u45ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case140<!>
   case140 checkType { check<Char>()}
}
// TESTCASE NUMBER: 141
fun case141(){
   val case141 = '\u4600'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case141<!>
   case141 checkType { check<Char>()}
}
// TESTCASE NUMBER: 142
fun case142(){
   val case142 = '\u46ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case142<!>
   case142 checkType { check<Char>()}
}
// TESTCASE NUMBER: 143
fun case143(){
   val case143 = '\u4700'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case143<!>
   case143 checkType { check<Char>()}
}
// TESTCASE NUMBER: 144
fun case144(){
   val case144 = '\u47ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case144<!>
   case144 checkType { check<Char>()}
}
// TESTCASE NUMBER: 145
fun case145(){
   val case145 = '\u4800'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case145<!>
   case145 checkType { check<Char>()}
}
// TESTCASE NUMBER: 146
fun case146(){
   val case146 = '\u48ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case146<!>
   case146 checkType { check<Char>()}
}
// TESTCASE NUMBER: 147
fun case147(){
   val case147 = '\u4900'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case147<!>
   case147 checkType { check<Char>()}
}
// TESTCASE NUMBER: 148
fun case148(){
   val case148 = '\u49ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case148<!>
   case148 checkType { check<Char>()}
}
// TESTCASE NUMBER: 149
fun case149(){
   val case149 = '\u4a00'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case149<!>
   case149 checkType { check<Char>()}
}
// TESTCASE NUMBER: 150
fun case150(){
   val case150 = '\u4aff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case150<!>
   case150 checkType { check<Char>()}
}
// TESTCASE NUMBER: 151
fun case151(){
   val case151 = '\u4b00'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case151<!>
   case151 checkType { check<Char>()}
}
// TESTCASE NUMBER: 152
fun case152(){
   val case152 = '\u4bff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case152<!>
   case152 checkType { check<Char>()}
}
// TESTCASE NUMBER: 153
fun case153(){
   val case153 = '\u4c00'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case153<!>
   case153 checkType { check<Char>()}
}
// TESTCASE NUMBER: 154
fun case154(){
   val case154 = '\u4cff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case154<!>
   case154 checkType { check<Char>()}
}
// TESTCASE NUMBER: 155
fun case155(){
   val case155 = '\u4d00'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case155<!>
   case155 checkType { check<Char>()}
}
// TESTCASE NUMBER: 156
fun case156(){
   val case156 = '\u4dff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case156<!>
   case156 checkType { check<Char>()}
}
// TESTCASE NUMBER: 157
fun case157(){
   val case157 = '\u4e00'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case157<!>
   case157 checkType { check<Char>()}
}
// TESTCASE NUMBER: 158
fun case158(){
   val case158 = '\u4eff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case158<!>
   case158 checkType { check<Char>()}
}
// TESTCASE NUMBER: 159
fun case159(){
   val case159 = '\u4f00'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case159<!>
   case159 checkType { check<Char>()}
}
// TESTCASE NUMBER: 160
fun case160(){
   val case160 = '\u4fff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case160<!>
   case160 checkType { check<Char>()}
}
// TESTCASE NUMBER: 161
fun case161(){
   val case161 = '\u5000'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case161<!>
   case161 checkType { check<Char>()}
}
// TESTCASE NUMBER: 162
fun case162(){
   val case162 = '\u50ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case162<!>
   case162 checkType { check<Char>()}
}
// TESTCASE NUMBER: 163
fun case163(){
   val case163 = '\u5100'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case163<!>
   case163 checkType { check<Char>()}
}
// TESTCASE NUMBER: 164
fun case164(){
   val case164 = '\u51ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case164<!>
   case164 checkType { check<Char>()}
}
// TESTCASE NUMBER: 165
fun case165(){
   val case165 = '\u5200'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case165<!>
   case165 checkType { check<Char>()}
}
// TESTCASE NUMBER: 166
fun case166(){
   val case166 = '\u52ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case166<!>
   case166 checkType { check<Char>()}
}
// TESTCASE NUMBER: 167
fun case167(){
   val case167 = '\u5300'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case167<!>
   case167 checkType { check<Char>()}
}
// TESTCASE NUMBER: 168
fun case168(){
   val case168 = '\u53ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case168<!>
   case168 checkType { check<Char>()}
}
// TESTCASE NUMBER: 169
fun case169(){
   val case169 = '\u5400'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case169<!>
   case169 checkType { check<Char>()}
}
// TESTCASE NUMBER: 170
fun case170(){
   val case170 = '\u54ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case170<!>
   case170 checkType { check<Char>()}
}
// TESTCASE NUMBER: 171
fun case171(){
   val case171 = '\u5500'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case171<!>
   case171 checkType { check<Char>()}
}
// TESTCASE NUMBER: 172
fun case172(){
   val case172 = '\u55ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case172<!>
   case172 checkType { check<Char>()}
}
// TESTCASE NUMBER: 173
fun case173(){
   val case173 = '\u5600'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case173<!>
   case173 checkType { check<Char>()}
}
// TESTCASE NUMBER: 174
fun case174(){
   val case174 = '\u56ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case174<!>
   case174 checkType { check<Char>()}
}
// TESTCASE NUMBER: 175
fun case175(){
   val case175 = '\u5700'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case175<!>
   case175 checkType { check<Char>()}
}
// TESTCASE NUMBER: 176
fun case176(){
   val case176 = '\u57ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case176<!>
   case176 checkType { check<Char>()}
}
// TESTCASE NUMBER: 177
fun case177(){
   val case177 = '\u5800'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case177<!>
   case177 checkType { check<Char>()}
}
// TESTCASE NUMBER: 178
fun case178(){
   val case178 = '\u58ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case178<!>
   case178 checkType { check<Char>()}
}
// TESTCASE NUMBER: 179
fun case179(){
   val case179 = '\u5900'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case179<!>
   case179 checkType { check<Char>()}
}
// TESTCASE NUMBER: 180
fun case180(){
   val case180 = '\u59ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case180<!>
   case180 checkType { check<Char>()}
}
// TESTCASE NUMBER: 181
fun case181(){
   val case181 = '\u5a00'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case181<!>
   case181 checkType { check<Char>()}
}
// TESTCASE NUMBER: 182
fun case182(){
   val case182 = '\u5aff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case182<!>
   case182 checkType { check<Char>()}
}
// TESTCASE NUMBER: 183
fun case183(){
   val case183 = '\u5b00'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case183<!>
   case183 checkType { check<Char>()}
}
// TESTCASE NUMBER: 184
fun case184(){
   val case184 = '\u5bff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case184<!>
   case184 checkType { check<Char>()}
}
// TESTCASE NUMBER: 185
fun case185(){
   val case185 = '\u5c00'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case185<!>
   case185 checkType { check<Char>()}
}
// TESTCASE NUMBER: 186
fun case186(){
   val case186 = '\u5cff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case186<!>
   case186 checkType { check<Char>()}
}
// TESTCASE NUMBER: 187
fun case187(){
   val case187 = '\u5d00'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case187<!>
   case187 checkType { check<Char>()}
}
// TESTCASE NUMBER: 188
fun case188(){
   val case188 = '\u5dff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case188<!>
   case188 checkType { check<Char>()}
}
// TESTCASE NUMBER: 189
fun case189(){
   val case189 = '\u5e00'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case189<!>
   case189 checkType { check<Char>()}
}
// TESTCASE NUMBER: 190
fun case190(){
   val case190 = '\u5eff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case190<!>
   case190 checkType { check<Char>()}
}
// TESTCASE NUMBER: 191
fun case191(){
   val case191 = '\u5f00'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case191<!>
   case191 checkType { check<Char>()}
}
// TESTCASE NUMBER: 192
fun case192(){
   val case192 = '\u5fff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case192<!>
   case192 checkType { check<Char>()}
}
// TESTCASE NUMBER: 193
fun case193(){
   val case193 = '\u6000'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case193<!>
   case193 checkType { check<Char>()}
}
// TESTCASE NUMBER: 194
fun case194(){
   val case194 = '\u60ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case194<!>
   case194 checkType { check<Char>()}
}
// TESTCASE NUMBER: 195
fun case195(){
   val case195 = '\u6100'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case195<!>
   case195 checkType { check<Char>()}
}
// TESTCASE NUMBER: 196
fun case196(){
   val case196 = '\u61ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case196<!>
   case196 checkType { check<Char>()}
}
// TESTCASE NUMBER: 197
fun case197(){
   val case197 = '\u6200'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case197<!>
   case197 checkType { check<Char>()}
}
// TESTCASE NUMBER: 198
fun case198(){
   val case198 = '\u62ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case198<!>
   case198 checkType { check<Char>()}
}
// TESTCASE NUMBER: 199
fun case199(){
   val case199 = '\u6300'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case199<!>
   case199 checkType { check<Char>()}
}
// TESTCASE NUMBER: 200
fun case200(){
   val case200 = '\u63ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case200<!>
   case200 checkType { check<Char>()}
}
// TESTCASE NUMBER: 201
fun case201(){
   val case201 = '\u6400'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case201<!>
   case201 checkType { check<Char>()}
}
// TESTCASE NUMBER: 202
fun case202(){
   val case202 = '\u64ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case202<!>
   case202 checkType { check<Char>()}
}
// TESTCASE NUMBER: 203
fun case203(){
   val case203 = '\u6500'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case203<!>
   case203 checkType { check<Char>()}
}
// TESTCASE NUMBER: 204
fun case204(){
   val case204 = '\u65ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case204<!>
   case204 checkType { check<Char>()}
}
// TESTCASE NUMBER: 205
fun case205(){
   val case205 = '\u6600'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case205<!>
   case205 checkType { check<Char>()}
}
// TESTCASE NUMBER: 206
fun case206(){
   val case206 = '\u66ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case206<!>
   case206 checkType { check<Char>()}
}
// TESTCASE NUMBER: 207
fun case207(){
   val case207 = '\u6700'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case207<!>
   case207 checkType { check<Char>()}
}
// TESTCASE NUMBER: 208
fun case208(){
   val case208 = '\u67ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case208<!>
   case208 checkType { check<Char>()}
}
// TESTCASE NUMBER: 209
fun case209(){
   val case209 = '\u6800'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case209<!>
   case209 checkType { check<Char>()}
}
// TESTCASE NUMBER: 210
fun case210(){
   val case210 = '\u68ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case210<!>
   case210 checkType { check<Char>()}
}
// TESTCASE NUMBER: 211
fun case211(){
   val case211 = '\u6900'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case211<!>
   case211 checkType { check<Char>()}
}
// TESTCASE NUMBER: 212
fun case212(){
   val case212 = '\u69ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case212<!>
   case212 checkType { check<Char>()}
}
// TESTCASE NUMBER: 213
fun case213(){
   val case213 = '\u6a00'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case213<!>
   case213 checkType { check<Char>()}
}
// TESTCASE NUMBER: 214
fun case214(){
   val case214 = '\u6aff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case214<!>
   case214 checkType { check<Char>()}
}
// TESTCASE NUMBER: 215
fun case215(){
   val case215 = '\u6b00'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case215<!>
   case215 checkType { check<Char>()}
}
// TESTCASE NUMBER: 216
fun case216(){
   val case216 = '\u6bff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case216<!>
   case216 checkType { check<Char>()}
}
// TESTCASE NUMBER: 217
fun case217(){
   val case217 = '\u6c00'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case217<!>
   case217 checkType { check<Char>()}
}
// TESTCASE NUMBER: 218
fun case218(){
   val case218 = '\u6cff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case218<!>
   case218 checkType { check<Char>()}
}
// TESTCASE NUMBER: 219
fun case219(){
   val case219 = '\u6d00'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case219<!>
   case219 checkType { check<Char>()}
}
// TESTCASE NUMBER: 220
fun case220(){
   val case220 = '\u6dff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case220<!>
   case220 checkType { check<Char>()}
}
// TESTCASE NUMBER: 221
fun case221(){
   val case221 = '\u6e00'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case221<!>
   case221 checkType { check<Char>()}
}
// TESTCASE NUMBER: 222
fun case222(){
   val case222 = '\u6eff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case222<!>
   case222 checkType { check<Char>()}
}
// TESTCASE NUMBER: 223
fun case223(){
   val case223 = '\u6f00'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case223<!>
   case223 checkType { check<Char>()}
}
// TESTCASE NUMBER: 224
fun case224(){
   val case224 = '\u6fff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case224<!>
   case224 checkType { check<Char>()}
}
// TESTCASE NUMBER: 225
fun case225(){
   val case225 = '\u7000'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case225<!>
   case225 checkType { check<Char>()}
}
// TESTCASE NUMBER: 226
fun case226(){
   val case226 = '\u70ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case226<!>
   case226 checkType { check<Char>()}
}
// TESTCASE NUMBER: 227
fun case227(){
   val case227 = '\u7100'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case227<!>
   case227 checkType { check<Char>()}
}
// TESTCASE NUMBER: 228
fun case228(){
   val case228 = '\u71ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case228<!>
   case228 checkType { check<Char>()}
}
// TESTCASE NUMBER: 229
fun case229(){
   val case229 = '\u7200'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case229<!>
   case229 checkType { check<Char>()}
}
// TESTCASE NUMBER: 230
fun case230(){
   val case230 = '\u72ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case230<!>
   case230 checkType { check<Char>()}
}
// TESTCASE NUMBER: 231
fun case231(){
   val case231 = '\u7300'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case231<!>
   case231 checkType { check<Char>()}
}
// TESTCASE NUMBER: 232
fun case232(){
   val case232 = '\u73ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case232<!>
   case232 checkType { check<Char>()}
}
// TESTCASE NUMBER: 233
fun case233(){
   val case233 = '\u7400'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case233<!>
   case233 checkType { check<Char>()}
}
// TESTCASE NUMBER: 234
fun case234(){
   val case234 = '\u74ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case234<!>
   case234 checkType { check<Char>()}
}
// TESTCASE NUMBER: 235
fun case235(){
   val case235 = '\u7500'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case235<!>
   case235 checkType { check<Char>()}
}
// TESTCASE NUMBER: 236
fun case236(){
   val case236 = '\u75ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case236<!>
   case236 checkType { check<Char>()}
}
// TESTCASE NUMBER: 237
fun case237(){
   val case237 = '\u7600'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case237<!>
   case237 checkType { check<Char>()}
}
// TESTCASE NUMBER: 238
fun case238(){
   val case238 = '\u76ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case238<!>
   case238 checkType { check<Char>()}
}
// TESTCASE NUMBER: 239
fun case239(){
   val case239 = '\u7700'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case239<!>
   case239 checkType { check<Char>()}
}
// TESTCASE NUMBER: 240
fun case240(){
   val case240 = '\u77ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case240<!>
   case240 checkType { check<Char>()}
}
// TESTCASE NUMBER: 241
fun case241(){
   val case241 = '\u7800'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case241<!>
   case241 checkType { check<Char>()}
}
// TESTCASE NUMBER: 242
fun case242(){
   val case242 = '\u78ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case242<!>
   case242 checkType { check<Char>()}
}
// TESTCASE NUMBER: 243
fun case243(){
   val case243 = '\u7900'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case243<!>
   case243 checkType { check<Char>()}
}
// TESTCASE NUMBER: 244
fun case244(){
   val case244 = '\u79ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case244<!>
   case244 checkType { check<Char>()}
}
// TESTCASE NUMBER: 245
fun case245(){
   val case245 = '\u7a00'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case245<!>
   case245 checkType { check<Char>()}
}
// TESTCASE NUMBER: 246
fun case246(){
   val case246 = '\u7aff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case246<!>
   case246 checkType { check<Char>()}
}
// TESTCASE NUMBER: 247
fun case247(){
   val case247 = '\u7b00'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case247<!>
   case247 checkType { check<Char>()}
}
// TESTCASE NUMBER: 248
fun case248(){
   val case248 = '\u7bff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case248<!>
   case248 checkType { check<Char>()}
}
// TESTCASE NUMBER: 249
fun case249(){
   val case249 = '\u7c00'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case249<!>
   case249 checkType { check<Char>()}
}
// TESTCASE NUMBER: 250
fun case250(){
   val case250 = '\u7cff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case250<!>
   case250 checkType { check<Char>()}
}
// TESTCASE NUMBER: 251
fun case251(){
   val case251 = '\u7d00'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case251<!>
   case251 checkType { check<Char>()}
}
// TESTCASE NUMBER: 252
fun case252(){
   val case252 = '\u7dff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case252<!>
   case252 checkType { check<Char>()}
}
// TESTCASE NUMBER: 253
fun case253(){
   val case253 = '\u7e00'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case253<!>
   case253 checkType { check<Char>()}
}
// TESTCASE NUMBER: 254
fun case254(){
   val case254 = '\u7eff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case254<!>
   case254 checkType { check<Char>()}
}
// TESTCASE NUMBER: 255
fun case255(){
   val case255 = '\u7f00'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case255<!>
   case255 checkType { check<Char>()}
}
// TESTCASE NUMBER: 256
fun case256(){
   val case256 = '\u7fff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case256<!>
   case256 checkType { check<Char>()}
}
// TESTCASE NUMBER: 257
fun case257(){
   val case257 = '\u8000'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case257<!>
   case257 checkType { check<Char>()}
}
// TESTCASE NUMBER: 258
fun case258(){
   val case258 = '\u80ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case258<!>
   case258 checkType { check<Char>()}
}
// TESTCASE NUMBER: 259
fun case259(){
   val case259 = '\u8100'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case259<!>
   case259 checkType { check<Char>()}
}
// TESTCASE NUMBER: 260
fun case260(){
   val case260 = '\u81ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case260<!>
   case260 checkType { check<Char>()}
}
// TESTCASE NUMBER: 261
fun case261(){
   val case261 = '\u8200'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case261<!>
   case261 checkType { check<Char>()}
}
// TESTCASE NUMBER: 262
fun case262(){
   val case262 = '\u82ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case262<!>
   case262 checkType { check<Char>()}
}
// TESTCASE NUMBER: 263
fun case263(){
   val case263 = '\u8300'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case263<!>
   case263 checkType { check<Char>()}
}
// TESTCASE NUMBER: 264
fun case264(){
   val case264 = '\u83ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case264<!>
   case264 checkType { check<Char>()}
}
// TESTCASE NUMBER: 265
fun case265(){
   val case265 = '\u8400'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case265<!>
   case265 checkType { check<Char>()}
}
// TESTCASE NUMBER: 266
fun case266(){
   val case266 = '\u84ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case266<!>
   case266 checkType { check<Char>()}
}
// TESTCASE NUMBER: 267
fun case267(){
   val case267 = '\u8500'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case267<!>
   case267 checkType { check<Char>()}
}
// TESTCASE NUMBER: 268
fun case268(){
   val case268 = '\u85ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case268<!>
   case268 checkType { check<Char>()}
}
// TESTCASE NUMBER: 269
fun case269(){
   val case269 = '\u8600'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case269<!>
   case269 checkType { check<Char>()}
}
// TESTCASE NUMBER: 270
fun case270(){
   val case270 = '\u86ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case270<!>
   case270 checkType { check<Char>()}
}
// TESTCASE NUMBER: 271
fun case271(){
   val case271 = '\u8700'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case271<!>
   case271 checkType { check<Char>()}
}
// TESTCASE NUMBER: 272
fun case272(){
   val case272 = '\u87ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case272<!>
   case272 checkType { check<Char>()}
}
// TESTCASE NUMBER: 273
fun case273(){
   val case273 = '\u8800'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case273<!>
   case273 checkType { check<Char>()}
}
// TESTCASE NUMBER: 274
fun case274(){
   val case274 = '\u88ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case274<!>
   case274 checkType { check<Char>()}
}
// TESTCASE NUMBER: 275
fun case275(){
   val case275 = '\u8900'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case275<!>
   case275 checkType { check<Char>()}
}
// TESTCASE NUMBER: 276
fun case276(){
   val case276 = '\u89ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case276<!>
   case276 checkType { check<Char>()}
}
// TESTCASE NUMBER: 277
fun case277(){
   val case277 = '\u8a00'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case277<!>
   case277 checkType { check<Char>()}
}
// TESTCASE NUMBER: 278
fun case278(){
   val case278 = '\u8aff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case278<!>
   case278 checkType { check<Char>()}
}
// TESTCASE NUMBER: 279
fun case279(){
   val case279 = '\u8b00'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case279<!>
   case279 checkType { check<Char>()}
}
// TESTCASE NUMBER: 280
fun case280(){
   val case280 = '\u8bff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case280<!>
   case280 checkType { check<Char>()}
}
// TESTCASE NUMBER: 281
fun case281(){
   val case281 = '\u8c00'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case281<!>
   case281 checkType { check<Char>()}
}
// TESTCASE NUMBER: 282
fun case282(){
   val case282 = '\u8cff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case282<!>
   case282 checkType { check<Char>()}
}
// TESTCASE NUMBER: 283
fun case283(){
   val case283 = '\u8d00'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case283<!>
   case283 checkType { check<Char>()}
}
// TESTCASE NUMBER: 284
fun case284(){
   val case284 = '\u8dff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case284<!>
   case284 checkType { check<Char>()}
}
// TESTCASE NUMBER: 285
fun case285(){
   val case285 = '\u8e00'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case285<!>
   case285 checkType { check<Char>()}
}
// TESTCASE NUMBER: 286
fun case286(){
   val case286 = '\u8eff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case286<!>
   case286 checkType { check<Char>()}
}
// TESTCASE NUMBER: 287
fun case287(){
   val case287 = '\u8f00'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case287<!>
   case287 checkType { check<Char>()}
}
// TESTCASE NUMBER: 288
fun case288(){
   val case288 = '\u8fff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case288<!>
   case288 checkType { check<Char>()}
}
// TESTCASE NUMBER: 289
fun case289(){
   val case289 = '\u9000'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case289<!>
   case289 checkType { check<Char>()}
}
// TESTCASE NUMBER: 290
fun case290(){
   val case290 = '\u90ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case290<!>
   case290 checkType { check<Char>()}
}
// TESTCASE NUMBER: 291
fun case291(){
   val case291 = '\u9100'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case291<!>
   case291 checkType { check<Char>()}
}
// TESTCASE NUMBER: 292
fun case292(){
   val case292 = '\u91ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case292<!>
   case292 checkType { check<Char>()}
}
// TESTCASE NUMBER: 293
fun case293(){
   val case293 = '\u9200'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case293<!>
   case293 checkType { check<Char>()}
}
// TESTCASE NUMBER: 294
fun case294(){
   val case294 = '\u92ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case294<!>
   case294 checkType { check<Char>()}
}
// TESTCASE NUMBER: 295
fun case295(){
   val case295 = '\u9300'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case295<!>
   case295 checkType { check<Char>()}
}
// TESTCASE NUMBER: 296
fun case296(){
   val case296 = '\u93ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case296<!>
   case296 checkType { check<Char>()}
}
// TESTCASE NUMBER: 297
fun case297(){
   val case297 = '\u9400'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case297<!>
   case297 checkType { check<Char>()}
}
// TESTCASE NUMBER: 298
fun case298(){
   val case298 = '\u94ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case298<!>
   case298 checkType { check<Char>()}
}
// TESTCASE NUMBER: 299
fun case299(){
   val case299 = '\u9500'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case299<!>
   case299 checkType { check<Char>()}
}
// TESTCASE NUMBER: 300
fun case300(){
   val case300 = '\u95ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case300<!>
   case300 checkType { check<Char>()}
}
// TESTCASE NUMBER: 301
fun case301(){
   val case301 = '\u9600'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case301<!>
   case301 checkType { check<Char>()}
}
// TESTCASE NUMBER: 302
fun case302(){
   val case302 = '\u96ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case302<!>
   case302 checkType { check<Char>()}
}
// TESTCASE NUMBER: 303
fun case303(){
   val case303 = '\u9700'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case303<!>
   case303 checkType { check<Char>()}
}
// TESTCASE NUMBER: 304
fun case304(){
   val case304 = '\u97ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case304<!>
   case304 checkType { check<Char>()}
}
// TESTCASE NUMBER: 305
fun case305(){
   val case305 = '\u9800'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case305<!>
   case305 checkType { check<Char>()}
}
// TESTCASE NUMBER: 306
fun case306(){
   val case306 = '\u98ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case306<!>
   case306 checkType { check<Char>()}
}
// TESTCASE NUMBER: 307
fun case307(){
   val case307 = '\u9900'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case307<!>
   case307 checkType { check<Char>()}
}
// TESTCASE NUMBER: 308
fun case308(){
   val case308 = '\u99ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case308<!>
   case308 checkType { check<Char>()}
}
// TESTCASE NUMBER: 309
fun case309(){
   val case309 = '\u9a00'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case309<!>
   case309 checkType { check<Char>()}
}
// TESTCASE NUMBER: 310
fun case310(){
   val case310 = '\u9aff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case310<!>
   case310 checkType { check<Char>()}
}
// TESTCASE NUMBER: 311
fun case311(){
   val case311 = '\u9b00'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case311<!>
   case311 checkType { check<Char>()}
}
// TESTCASE NUMBER: 312
fun case312(){
   val case312 = '\u9bff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case312<!>
   case312 checkType { check<Char>()}
}
// TESTCASE NUMBER: 313
fun case313(){
   val case313 = '\u9c00'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case313<!>
   case313 checkType { check<Char>()}
}
// TESTCASE NUMBER: 314
fun case314(){
   val case314 = '\u9cff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case314<!>
   case314 checkType { check<Char>()}
}
// TESTCASE NUMBER: 315
fun case315(){
   val case315 = '\u9d00'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case315<!>
   case315 checkType { check<Char>()}
}
// TESTCASE NUMBER: 316
fun case316(){
   val case316 = '\u9dff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case316<!>
   case316 checkType { check<Char>()}
}
// TESTCASE NUMBER: 317
fun case317(){
   val case317 = '\u9e00'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case317<!>
   case317 checkType { check<Char>()}
}
// TESTCASE NUMBER: 318
fun case318(){
   val case318 = '\u9eff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case318<!>
   case318 checkType { check<Char>()}
}
// TESTCASE NUMBER: 319
fun case319(){
   val case319 = '\u9f00'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case319<!>
   case319 checkType { check<Char>()}
}
// TESTCASE NUMBER: 320
fun case320(){
   val case320 = '\u9fff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case320<!>
   case320 checkType { check<Char>()}
}
// TESTCASE NUMBER: 321
fun case321(){
   val case321 = '\ua000'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case321<!>
   case321 checkType { check<Char>()}
}
// TESTCASE NUMBER: 322
fun case322(){
   val case322 = '\ua0ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case322<!>
   case322 checkType { check<Char>()}
}
// TESTCASE NUMBER: 323
fun case323(){
   val case323 = '\ua100'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case323<!>
   case323 checkType { check<Char>()}
}
// TESTCASE NUMBER: 324
fun case324(){
   val case324 = '\ua1ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case324<!>
   case324 checkType { check<Char>()}
}
// TESTCASE NUMBER: 325
fun case325(){
   val case325 = '\ua200'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case325<!>
   case325 checkType { check<Char>()}
}
// TESTCASE NUMBER: 326
fun case326(){
   val case326 = '\ua2ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case326<!>
   case326 checkType { check<Char>()}
}
// TESTCASE NUMBER: 327
fun case327(){
   val case327 = '\ua300'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case327<!>
   case327 checkType { check<Char>()}
}
// TESTCASE NUMBER: 328
fun case328(){
   val case328 = '\ua3ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case328<!>
   case328 checkType { check<Char>()}
}
// TESTCASE NUMBER: 329
fun case329(){
   val case329 = '\ua400'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case329<!>
   case329 checkType { check<Char>()}
}
// TESTCASE NUMBER: 330
fun case330(){
   val case330 = '\ua4ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case330<!>
   case330 checkType { check<Char>()}
}
// TESTCASE NUMBER: 331
fun case331(){
   val case331 = '\ua500'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case331<!>
   case331 checkType { check<Char>()}
}
// TESTCASE NUMBER: 332
fun case332(){
   val case332 = '\ua5ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case332<!>
   case332 checkType { check<Char>()}
}
// TESTCASE NUMBER: 333
fun case333(){
   val case333 = '\ua600'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case333<!>
   case333 checkType { check<Char>()}
}
// TESTCASE NUMBER: 334
fun case334(){
   val case334 = '\ua6ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case334<!>
   case334 checkType { check<Char>()}
}
// TESTCASE NUMBER: 335
fun case335(){
   val case335 = '\ua700'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case335<!>
   case335 checkType { check<Char>()}
}
// TESTCASE NUMBER: 336
fun case336(){
   val case336 = '\ua7ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case336<!>
   case336 checkType { check<Char>()}
}
// TESTCASE NUMBER: 337
fun case337(){
   val case337 = '\ua800'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case337<!>
   case337 checkType { check<Char>()}
}
// TESTCASE NUMBER: 338
fun case338(){
   val case338 = '\ua8ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case338<!>
   case338 checkType { check<Char>()}
}
// TESTCASE NUMBER: 339
fun case339(){
   val case339 = '\ua900'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case339<!>
   case339 checkType { check<Char>()}
}
// TESTCASE NUMBER: 340
fun case340(){
   val case340 = '\ua9ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case340<!>
   case340 checkType { check<Char>()}
}
// TESTCASE NUMBER: 341
fun case341(){
   val case341 = '\uaa00'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case341<!>
   case341 checkType { check<Char>()}
}
// TESTCASE NUMBER: 342
fun case342(){
   val case342 = '\uaaff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case342<!>
   case342 checkType { check<Char>()}
}
// TESTCASE NUMBER: 343
fun case343(){
   val case343 = '\uab00'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case343<!>
   case343 checkType { check<Char>()}
}
// TESTCASE NUMBER: 344
fun case344(){
   val case344 = '\uabff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case344<!>
   case344 checkType { check<Char>()}
}
// TESTCASE NUMBER: 345
fun case345(){
   val case345 = '\uac00'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case345<!>
   case345 checkType { check<Char>()}
}
// TESTCASE NUMBER: 346
fun case346(){
   val case346 = '\uacff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case346<!>
   case346 checkType { check<Char>()}
}
// TESTCASE NUMBER: 347
fun case347(){
   val case347 = '\uad00'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case347<!>
   case347 checkType { check<Char>()}
}
// TESTCASE NUMBER: 348
fun case348(){
   val case348 = '\uadff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case348<!>
   case348 checkType { check<Char>()}
}
// TESTCASE NUMBER: 349
fun case349(){
   val case349 = '\uae00'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case349<!>
   case349 checkType { check<Char>()}
}
// TESTCASE NUMBER: 350
fun case350(){
   val case350 = '\uaeff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case350<!>
   case350 checkType { check<Char>()}
}
// TESTCASE NUMBER: 351
fun case351(){
   val case351 = '\uaf00'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case351<!>
   case351 checkType { check<Char>()}
}
// TESTCASE NUMBER: 352
fun case352(){
   val case352 = '\uafff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case352<!>
   case352 checkType { check<Char>()}
}
// TESTCASE NUMBER: 353
fun case353(){
   val case353 = '\ub000'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case353<!>
   case353 checkType { check<Char>()}
}
// TESTCASE NUMBER: 354
fun case354(){
   val case354 = '\ub0ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case354<!>
   case354 checkType { check<Char>()}
}
// TESTCASE NUMBER: 355
fun case355(){
   val case355 = '\ub100'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case355<!>
   case355 checkType { check<Char>()}
}
// TESTCASE NUMBER: 356
fun case356(){
   val case356 = '\ub1ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case356<!>
   case356 checkType { check<Char>()}
}
// TESTCASE NUMBER: 357
fun case357(){
   val case357 = '\ub200'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case357<!>
   case357 checkType { check<Char>()}
}
// TESTCASE NUMBER: 358
fun case358(){
   val case358 = '\ub2ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case358<!>
   case358 checkType { check<Char>()}
}
// TESTCASE NUMBER: 359
fun case359(){
   val case359 = '\ub300'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case359<!>
   case359 checkType { check<Char>()}
}
// TESTCASE NUMBER: 360
fun case360(){
   val case360 = '\ub3ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case360<!>
   case360 checkType { check<Char>()}
}
// TESTCASE NUMBER: 361
fun case361(){
   val case361 = '\ub400'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case361<!>
   case361 checkType { check<Char>()}
}
// TESTCASE NUMBER: 362
fun case362(){
   val case362 = '\ub4ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case362<!>
   case362 checkType { check<Char>()}
}
// TESTCASE NUMBER: 363
fun case363(){
   val case363 = '\ub500'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case363<!>
   case363 checkType { check<Char>()}
}
// TESTCASE NUMBER: 364
fun case364(){
   val case364 = '\ub5ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case364<!>
   case364 checkType { check<Char>()}
}
// TESTCASE NUMBER: 365
fun case365(){
   val case365 = '\ub600'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case365<!>
   case365 checkType { check<Char>()}
}
// TESTCASE NUMBER: 366
fun case366(){
   val case366 = '\ub6ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case366<!>
   case366 checkType { check<Char>()}
}
// TESTCASE NUMBER: 367
fun case367(){
   val case367 = '\ub700'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case367<!>
   case367 checkType { check<Char>()}
}
// TESTCASE NUMBER: 368
fun case368(){
   val case368 = '\ub7ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case368<!>
   case368 checkType { check<Char>()}
}
// TESTCASE NUMBER: 369
fun case369(){
   val case369 = '\ub800'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case369<!>
   case369 checkType { check<Char>()}
}
// TESTCASE NUMBER: 370
fun case370(){
   val case370 = '\ub8ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case370<!>
   case370 checkType { check<Char>()}
}
// TESTCASE NUMBER: 371
fun case371(){
   val case371 = '\ub900'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case371<!>
   case371 checkType { check<Char>()}
}
// TESTCASE NUMBER: 372
fun case372(){
   val case372 = '\ub9ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case372<!>
   case372 checkType { check<Char>()}
}
// TESTCASE NUMBER: 373
fun case373(){
   val case373 = '\uba00'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case373<!>
   case373 checkType { check<Char>()}
}
// TESTCASE NUMBER: 374
fun case374(){
   val case374 = '\ubaff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case374<!>
   case374 checkType { check<Char>()}
}
// TESTCASE NUMBER: 375
fun case375(){
   val case375 = '\ubb00'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case375<!>
   case375 checkType { check<Char>()}
}
// TESTCASE NUMBER: 376
fun case376(){
   val case376 = '\ubbff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case376<!>
   case376 checkType { check<Char>()}
}
// TESTCASE NUMBER: 377
fun case377(){
   val case377 = '\ubc00'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case377<!>
   case377 checkType { check<Char>()}
}
// TESTCASE NUMBER: 378
fun case378(){
   val case378 = '\ubcff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case378<!>
   case378 checkType { check<Char>()}
}
// TESTCASE NUMBER: 379
fun case379(){
   val case379 = '\ubd00'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case379<!>
   case379 checkType { check<Char>()}
}
// TESTCASE NUMBER: 380
fun case380(){
   val case380 = '\ubdff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case380<!>
   case380 checkType { check<Char>()}
}
// TESTCASE NUMBER: 381
fun case381(){
   val case381 = '\ube00'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case381<!>
   case381 checkType { check<Char>()}
}
// TESTCASE NUMBER: 382
fun case382(){
   val case382 = '\ubeff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case382<!>
   case382 checkType { check<Char>()}
}
// TESTCASE NUMBER: 383
fun case383(){
   val case383 = '\ubf00'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case383<!>
   case383 checkType { check<Char>()}
}
// TESTCASE NUMBER: 384
fun case384(){
   val case384 = '\ubfff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case384<!>
   case384 checkType { check<Char>()}
}
// TESTCASE NUMBER: 385
fun case385(){
   val case385 = '\uc000'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case385<!>
   case385 checkType { check<Char>()}
}
// TESTCASE NUMBER: 386
fun case386(){
   val case386 = '\uc0ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case386<!>
   case386 checkType { check<Char>()}
}
// TESTCASE NUMBER: 387
fun case387(){
   val case387 = '\uc100'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case387<!>
   case387 checkType { check<Char>()}
}
// TESTCASE NUMBER: 388
fun case388(){
   val case388 = '\uc1ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case388<!>
   case388 checkType { check<Char>()}
}
// TESTCASE NUMBER: 389
fun case389(){
   val case389 = '\uc200'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case389<!>
   case389 checkType { check<Char>()}
}
// TESTCASE NUMBER: 390
fun case390(){
   val case390 = '\uc2ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case390<!>
   case390 checkType { check<Char>()}
}
// TESTCASE NUMBER: 391
fun case391(){
   val case391 = '\uc300'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case391<!>
   case391 checkType { check<Char>()}
}
// TESTCASE NUMBER: 392
fun case392(){
   val case392 = '\uc3ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case392<!>
   case392 checkType { check<Char>()}
}
// TESTCASE NUMBER: 393
fun case393(){
   val case393 = '\uc400'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case393<!>
   case393 checkType { check<Char>()}
}
// TESTCASE NUMBER: 394
fun case394(){
   val case394 = '\uc4ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case394<!>
   case394 checkType { check<Char>()}
}
// TESTCASE NUMBER: 395
fun case395(){
   val case395 = '\uc500'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case395<!>
   case395 checkType { check<Char>()}
}
// TESTCASE NUMBER: 396
fun case396(){
   val case396 = '\uc5ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case396<!>
   case396 checkType { check<Char>()}
}
// TESTCASE NUMBER: 397
fun case397(){
   val case397 = '\uc600'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case397<!>
   case397 checkType { check<Char>()}
}
// TESTCASE NUMBER: 398
fun case398(){
   val case398 = '\uc6ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case398<!>
   case398 checkType { check<Char>()}
}
// TESTCASE NUMBER: 399
fun case399(){
   val case399 = '\uc700'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case399<!>
   case399 checkType { check<Char>()}
}
// TESTCASE NUMBER: 400
fun case400(){
   val case400 = '\uc7ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case400<!>
   case400 checkType { check<Char>()}
}
// TESTCASE NUMBER: 401
fun case401(){
   val case401 = '\uc800'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case401<!>
   case401 checkType { check<Char>()}
}
// TESTCASE NUMBER: 402
fun case402(){
   val case402 = '\uc8ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case402<!>
   case402 checkType { check<Char>()}
}
// TESTCASE NUMBER: 403
fun case403(){
   val case403 = '\uc900'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case403<!>
   case403 checkType { check<Char>()}
}
// TESTCASE NUMBER: 404
fun case404(){
   val case404 = '\uc9ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case404<!>
   case404 checkType { check<Char>()}
}
// TESTCASE NUMBER: 405
fun case405(){
   val case405 = '\uca00'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case405<!>
   case405 checkType { check<Char>()}
}
// TESTCASE NUMBER: 406
fun case406(){
   val case406 = '\ucaff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case406<!>
   case406 checkType { check<Char>()}
}
// TESTCASE NUMBER: 407
fun case407(){
   val case407 = '\ucb00'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case407<!>
   case407 checkType { check<Char>()}
}
// TESTCASE NUMBER: 408
fun case408(){
   val case408 = '\ucbff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case408<!>
   case408 checkType { check<Char>()}
}
// TESTCASE NUMBER: 409
fun case409(){
   val case409 = '\ucc00'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case409<!>
   case409 checkType { check<Char>()}
}
// TESTCASE NUMBER: 410
fun case410(){
   val case410 = '\uccff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case410<!>
   case410 checkType { check<Char>()}
}
// TESTCASE NUMBER: 411
fun case411(){
   val case411 = '\ucd00'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case411<!>
   case411 checkType { check<Char>()}
}
// TESTCASE NUMBER: 412
fun case412(){
   val case412 = '\ucdff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case412<!>
   case412 checkType { check<Char>()}
}
// TESTCASE NUMBER: 413
fun case413(){
   val case413 = '\uce00'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case413<!>
   case413 checkType { check<Char>()}
}
// TESTCASE NUMBER: 414
fun case414(){
   val case414 = '\uceff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case414<!>
   case414 checkType { check<Char>()}
}
// TESTCASE NUMBER: 415
fun case415(){
   val case415 = '\ucf00'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case415<!>
   case415 checkType { check<Char>()}
}
// TESTCASE NUMBER: 416
fun case416(){
   val case416 = '\ucfff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case416<!>
   case416 checkType { check<Char>()}
}
// TESTCASE NUMBER: 417
fun case417(){
   val case417 = '\ud000'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case417<!>
   case417 checkType { check<Char>()}
}
// TESTCASE NUMBER: 418
fun case418(){
   val case418 = '\ud0ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case418<!>
   case418 checkType { check<Char>()}
}
// TESTCASE NUMBER: 419
fun case419(){
   val case419 = '\ud100'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case419<!>
   case419 checkType { check<Char>()}
}
// TESTCASE NUMBER: 420
fun case420(){
   val case420 = '\ud1ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case420<!>
   case420 checkType { check<Char>()}
}
// TESTCASE NUMBER: 421
fun case421(){
   val case421 = '\ud200'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case421<!>
   case421 checkType { check<Char>()}
}
// TESTCASE NUMBER: 422
fun case422(){
   val case422 = '\ud2ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case422<!>
   case422 checkType { check<Char>()}
}
// TESTCASE NUMBER: 423
fun case423(){
   val case423 = '\ud300'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case423<!>
   case423 checkType { check<Char>()}
}
// TESTCASE NUMBER: 424
fun case424(){
   val case424 = '\ud3ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case424<!>
   case424 checkType { check<Char>()}
}
// TESTCASE NUMBER: 425
fun case425(){
   val case425 = '\ud400'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case425<!>
   case425 checkType { check<Char>()}
}
// TESTCASE NUMBER: 426
fun case426(){
   val case426 = '\ud4ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case426<!>
   case426 checkType { check<Char>()}
}
// TESTCASE NUMBER: 427
fun case427(){
   val case427 = '\ud500'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case427<!>
   case427 checkType { check<Char>()}
}
// TESTCASE NUMBER: 428
fun case428(){
   val case428 = '\ud5ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case428<!>
   case428 checkType { check<Char>()}
}
// TESTCASE NUMBER: 429
fun case429(){
   val case429 = '\ud600'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case429<!>
   case429 checkType { check<Char>()}
}
// TESTCASE NUMBER: 430
fun case430(){
   val case430 = '\ud6ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case430<!>
   case430 checkType { check<Char>()}
}
// TESTCASE NUMBER: 431
fun case431(){
   val case431 = '\ud700'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case431<!>
   case431 checkType { check<Char>()}
}
// TESTCASE NUMBER: 432
fun case432(){
   val case432 = '\ud7ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case432<!>
   case432 checkType { check<Char>()}
}
// TESTCASE NUMBER: 433
fun case433(){
   val case433 = '\ud800'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case433<!>
   case433 checkType { check<Char>()}
}
// TESTCASE NUMBER: 434
fun case434(){
   val case434 = '\ud8ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case434<!>
   case434 checkType { check<Char>()}
}
// TESTCASE NUMBER: 435
fun case435(){
   val case435 = '\ud900'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case435<!>
   case435 checkType { check<Char>()}
}
// TESTCASE NUMBER: 436
fun case436(){
   val case436 = '\ud9ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case436<!>
   case436 checkType { check<Char>()}
}
// TESTCASE NUMBER: 437
fun case437(){
   val case437 = '\uda00'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case437<!>
   case437 checkType { check<Char>()}
}
// TESTCASE NUMBER: 438
fun case438(){
   val case438 = '\udaff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case438<!>
   case438 checkType { check<Char>()}
}
// TESTCASE NUMBER: 439
fun case439(){
   val case439 = '\udb00'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case439<!>
   case439 checkType { check<Char>()}
}
// TESTCASE NUMBER: 440
fun case440(){
   val case440 = '\udbff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case440<!>
   case440 checkType { check<Char>()}
}
// TESTCASE NUMBER: 441
fun case441(){
   val case441 = '\udc00'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case441<!>
   case441 checkType { check<Char>()}
}
// TESTCASE NUMBER: 442
fun case442(){
   val case442 = '\udcff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case442<!>
   case442 checkType { check<Char>()}
}
// TESTCASE NUMBER: 443
fun case443(){
   val case443 = '\udd00'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case443<!>
   case443 checkType { check<Char>()}
}
// TESTCASE NUMBER: 444
fun case444(){
   val case444 = '\uddff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case444<!>
   case444 checkType { check<Char>()}
}
// TESTCASE NUMBER: 445
fun case445(){
   val case445 = '\ude00'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case445<!>
   case445 checkType { check<Char>()}
}
// TESTCASE NUMBER: 446
fun case446(){
   val case446 = '\udeff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case446<!>
   case446 checkType { check<Char>()}
}
// TESTCASE NUMBER: 447
fun case447(){
   val case447 = '\udf00'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case447<!>
   case447 checkType { check<Char>()}
}
// TESTCASE NUMBER: 448
fun case448(){
   val case448 = '\udfff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case448<!>
   case448 checkType { check<Char>()}
}
// TESTCASE NUMBER: 449
fun case449(){
   val case449 = '\ue000'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case449<!>
   case449 checkType { check<Char>()}
}
// TESTCASE NUMBER: 450
fun case450(){
   val case450 = '\ue0ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case450<!>
   case450 checkType { check<Char>()}
}
// TESTCASE NUMBER: 451
fun case451(){
   val case451 = '\ue100'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case451<!>
   case451 checkType { check<Char>()}
}
// TESTCASE NUMBER: 452
fun case452(){
   val case452 = '\ue1ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case452<!>
   case452 checkType { check<Char>()}
}
// TESTCASE NUMBER: 453
fun case453(){
   val case453 = '\ue200'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case453<!>
   case453 checkType { check<Char>()}
}
// TESTCASE NUMBER: 454
fun case454(){
   val case454 = '\ue2ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case454<!>
   case454 checkType { check<Char>()}
}
// TESTCASE NUMBER: 455
fun case455(){
   val case455 = '\ue300'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case455<!>
   case455 checkType { check<Char>()}
}
// TESTCASE NUMBER: 456
fun case456(){
   val case456 = '\ue3ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case456<!>
   case456 checkType { check<Char>()}
}
// TESTCASE NUMBER: 457
fun case457(){
   val case457 = '\ue400'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case457<!>
   case457 checkType { check<Char>()}
}
// TESTCASE NUMBER: 458
fun case458(){
   val case458 = '\ue4ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case458<!>
   case458 checkType { check<Char>()}
}
// TESTCASE NUMBER: 459
fun case459(){
   val case459 = '\ue500'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case459<!>
   case459 checkType { check<Char>()}
}
// TESTCASE NUMBER: 460
fun case460(){
   val case460 = '\ue5ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case460<!>
   case460 checkType { check<Char>()}
}
// TESTCASE NUMBER: 461
fun case461(){
   val case461 = '\ue600'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case461<!>
   case461 checkType { check<Char>()}
}
// TESTCASE NUMBER: 462
fun case462(){
   val case462 = '\ue6ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case462<!>
   case462 checkType { check<Char>()}
}
// TESTCASE NUMBER: 463
fun case463(){
   val case463 = '\ue700'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case463<!>
   case463 checkType { check<Char>()}
}
// TESTCASE NUMBER: 464
fun case464(){
   val case464 = '\ue7ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case464<!>
   case464 checkType { check<Char>()}
}
// TESTCASE NUMBER: 465
fun case465(){
   val case465 = '\ue800'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case465<!>
   case465 checkType { check<Char>()}
}
// TESTCASE NUMBER: 466
fun case466(){
   val case466 = '\ue8ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case466<!>
   case466 checkType { check<Char>()}
}
// TESTCASE NUMBER: 467
fun case467(){
   val case467 = '\ue900'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case467<!>
   case467 checkType { check<Char>()}
}
// TESTCASE NUMBER: 468
fun case468(){
   val case468 = '\ue9ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case468<!>
   case468 checkType { check<Char>()}
}
// TESTCASE NUMBER: 469
fun case469(){
   val case469 = '\uea00'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case469<!>
   case469 checkType { check<Char>()}
}
// TESTCASE NUMBER: 470
fun case470(){
   val case470 = '\ueaff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case470<!>
   case470 checkType { check<Char>()}
}
// TESTCASE NUMBER: 471
fun case471(){
   val case471 = '\ueb00'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case471<!>
   case471 checkType { check<Char>()}
}
// TESTCASE NUMBER: 472
fun case472(){
   val case472 = '\uebff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case472<!>
   case472 checkType { check<Char>()}
}
// TESTCASE NUMBER: 473
fun case473(){
   val case473 = '\uec00'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case473<!>
   case473 checkType { check<Char>()}
}
// TESTCASE NUMBER: 474
fun case474(){
   val case474 = '\uecff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case474<!>
   case474 checkType { check<Char>()}
}
// TESTCASE NUMBER: 475
fun case475(){
   val case475 = '\ued00'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case475<!>
   case475 checkType { check<Char>()}
}
// TESTCASE NUMBER: 476
fun case476(){
   val case476 = '\uedff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case476<!>
   case476 checkType { check<Char>()}
}
// TESTCASE NUMBER: 477
fun case477(){
   val case477 = '\uee00'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case477<!>
   case477 checkType { check<Char>()}
}
// TESTCASE NUMBER: 478
fun case478(){
   val case478 = '\ueeff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case478<!>
   case478 checkType { check<Char>()}
}
// TESTCASE NUMBER: 479
fun case479(){
   val case479 = '\uef00'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case479<!>
   case479 checkType { check<Char>()}
}
// TESTCASE NUMBER: 480
fun case480(){
   val case480 = '\uefff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case480<!>
   case480 checkType { check<Char>()}
}
// TESTCASE NUMBER: 481
fun case481(){
   val case481 = '\uf000'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case481<!>
   case481 checkType { check<Char>()}
}
// TESTCASE NUMBER: 482
fun case482(){
   val case482 = '\uf0ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case482<!>
   case482 checkType { check<Char>()}
}
// TESTCASE NUMBER: 483
fun case483(){
   val case483 = '\uf100'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case483<!>
   case483 checkType { check<Char>()}
}
// TESTCASE NUMBER: 484
fun case484(){
   val case484 = '\uf1ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case484<!>
   case484 checkType { check<Char>()}
}
// TESTCASE NUMBER: 485
fun case485(){
   val case485 = '\uf200'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case485<!>
   case485 checkType { check<Char>()}
}
// TESTCASE NUMBER: 486
fun case486(){
   val case486 = '\uf2ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case486<!>
   case486 checkType { check<Char>()}
}
// TESTCASE NUMBER: 487
fun case487(){
   val case487 = '\uf300'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case487<!>
   case487 checkType { check<Char>()}
}
// TESTCASE NUMBER: 488
fun case488(){
   val case488 = '\uf3ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case488<!>
   case488 checkType { check<Char>()}
}
// TESTCASE NUMBER: 489
fun case489(){
   val case489 = '\uf400'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case489<!>
   case489 checkType { check<Char>()}
}
// TESTCASE NUMBER: 490
fun case490(){
   val case490 = '\uf4ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case490<!>
   case490 checkType { check<Char>()}
}
// TESTCASE NUMBER: 491
fun case491(){
   val case491 = '\uf500'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case491<!>
   case491 checkType { check<Char>()}
}
// TESTCASE NUMBER: 492
fun case492(){
   val case492 = '\uf5ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case492<!>
   case492 checkType { check<Char>()}
}
// TESTCASE NUMBER: 493
fun case493(){
   val case493 = '\uf600'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case493<!>
   case493 checkType { check<Char>()}
}
// TESTCASE NUMBER: 494
fun case494(){
   val case494 = '\uf6ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case494<!>
   case494 checkType { check<Char>()}
}
// TESTCASE NUMBER: 495
fun case495(){
   val case495 = '\uf700'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case495<!>
   case495 checkType { check<Char>()}
}
// TESTCASE NUMBER: 496
fun case496(){
   val case496 = '\uf7ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case496<!>
   case496 checkType { check<Char>()}
}
// TESTCASE NUMBER: 497
fun case497(){
   val case497 = '\uf800'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case497<!>
   case497 checkType { check<Char>()}
}
// TESTCASE NUMBER: 498
fun case498(){
   val case498 = '\uf8ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case498<!>
   case498 checkType { check<Char>()}
}
// TESTCASE NUMBER: 499
fun case499(){
   val case499 = '\uf900'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case499<!>
   case499 checkType { check<Char>()}
}
// TESTCASE NUMBER: 500
fun case500(){
   val case500 = '\uf9ff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case500<!>
   case500 checkType { check<Char>()}
}
// TESTCASE NUMBER: 501
fun case501(){
   val case501 = '\ufa00'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case501<!>
   case501 checkType { check<Char>()}
}
// TESTCASE NUMBER: 502
fun case502(){
   val case502 = '\ufaff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case502<!>
   case502 checkType { check<Char>()}
}
// TESTCASE NUMBER: 503
fun case503(){
   val case503 = '\ufb00'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case503<!>
   case503 checkType { check<Char>()}
}
// TESTCASE NUMBER: 504
fun case504(){
   val case504 = '\ufbff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case504<!>
   case504 checkType { check<Char>()}
}
// TESTCASE NUMBER: 505
fun case505(){
   val case505 = '\ufc00'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case505<!>
   case505 checkType { check<Char>()}
}
// TESTCASE NUMBER: 506
fun case506(){
   val case506 = '\ufcff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case506<!>
   case506 checkType { check<Char>()}
}
// TESTCASE NUMBER: 507
fun case507(){
   val case507 = '\ufd00'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case507<!>
   case507 checkType { check<Char>()}
}
// TESTCASE NUMBER: 508
fun case508(){
   val case508 = '\ufdff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case508<!>
   case508 checkType { check<Char>()}
}
// TESTCASE NUMBER: 509
fun case509(){
   val case509 = '\ufe00'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case509<!>
   case509 checkType { check<Char>()}
}
// TESTCASE NUMBER: 510
fun case510(){
   val case510 = '\ufeff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case510<!>
   case510 checkType { check<Char>()}
}
// TESTCASE NUMBER: 511
fun case511(){
   val case511 = '\uff00'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case511<!>
   case511 checkType { check<Char>()}
}
// TESTCASE NUMBER: 512
fun case512(){
   val case512 = '\uffff'
   <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>case512<!>
   case512 checkType { check<Char>()}
}
