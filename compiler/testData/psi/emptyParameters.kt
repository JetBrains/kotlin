// COMPILATION_ERRORS

fun fun1() {}
fun fun2(,) {}
fun fun3(normal: Type,) {}
fun fun4(, normal: Type) {}
fun fun5(normal: Type, ,) {}
fun fun6(normal: Type, , normal: Type) {}
fun fun7(normal: Type, , normal: Type,) {}
fun fun8(, , normal: Type) {}

fun fun9(justName,) {}
fun fun10(justName, ,) {}
fun fun11(normal: Type, , justName) {}
fun fun12(, justName,) {}
fun fun13(justName, , justName) {}

fun fun14( ,val property: Type,) {}
fun fun15(var property, ,) {}

fun fun16(, @Anno justName) {}
fun fun17(@DanglingAnno , @Anno normal: Type) {}
fun fun18(@DanglingAnno, @DanglingAnno) {}
fun fun19(@Anno justName, , normal: Type) {}

fun fun20(vararg , vararg justName, vararg normal: Type,) {}
fun fun21(vararg , ,) {}
fun fun22(, vararg justName) {}

fun fun23(: JustType) {}
fun fun24(: JustType normal: Type) {}
fun fun25(, : JustType) {}
fun fun26(: JustType, , justName: ) {}

fun fun27(normal: Type =, ) {}
fun fun28(normal: Type = default, ,) {}
fun fun29(normal: Type = @DanglingAnno) {}
fun fun30(normal: Type = @DanglingAnno, ,) {}

fun fun31(normal: @DanglingAnno) {}
fun fun32(normal: @DanglingAnno, ,) {}
fun fun33(normal: @DanglingAnno val property: @DanglingAnno =) {}

fun fun34(,=, ,) {}
fun fun35(= default, ,) {}
fun fun36(, : JustType = default, ,) {}

fun fun37(=) {}
fun fun38(:) {}
fun fun39(,:) {}
fun fun40(:,) {}
fun fun41(=,) {}
fun fun42(,=) {}
fun fun43(:=) {}
fun fun44(=:) {}
fun fun45(:,=) {}
fun fun46(=,:) {}
fun fun47(=, ,:) {}
fun fun48(, :, =,) {}

fun fun49(val) {}
fun fun50(val ,) {}
fun fun51(var , var) {}
