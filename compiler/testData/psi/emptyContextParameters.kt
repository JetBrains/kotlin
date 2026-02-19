// COMPILATION_ERRORS

context() fun fun1() {}
context(,) fun fun2() {}
context(normal: Type,) fun fun3() {}
context(, normal: Type) fun fun4() {}
context(normal: Type, ,) fun fun5() {}
context(normal: Type, , normal: Type) fun fun6() {}
context(normal: Type, , normal: Type,) fun fun7() {}
context(, , normal: Type) fun fun8() {}

context(justName,) fun fun9() {}
context(justName, ,) fun fun10() {}
context(normal: Type, , justName) fun fun11() {}
context(, justName,) fun fun12() {}
context(justName, , justName) fun fun13() {}

// actually `val`/`var` don't get parsed inside context parameter list
context( ,val property: Type,) fun fun14() {}
context(var property, ,) fun fun15() {}

context(, @Anno justName) fun fun16() {}
context(@DanglingAnno , @Anno normal: Type) fun fun17() {}
context(@DanglingAnno, @DanglingAnno) fun fun18() {}
context(@Anno justName, , normal: Type) fun fun19() {}

context(vararg , vararg justName, vararg normal: Type,) fun fun20() {}
context(vararg , ,) fun fun21() {}
context(, vararg justName) fun fun22() {}

context(: JustType) fun fun23() {}
context(: JustType normal: Type) fun fun24() {}
context(, : JustType) fun fun25() {}
context(: JustType, , justName: ) fun fun26() {}

context(normal: Type =, ) fun fun27() {}
context(normal: Type = default, ,) fun fun28() {}
context(normal: Type = @DanglingAnno) fun fun29() {}
context(normal: Type = @DanglingAnno, ,) fun fun30() {}

context(normal: @DanglingAnno) fun fun31() {}
context(normal: @DanglingAnno, ,) fun fun32() {}
context(normal: @DanglingAnno val property: @DanglingAnno =) fun fun33() {}

context(,=, ,) fun fun34() {}
context(= default, ,) fun fun35() {}
context(, : JustType = default, ,) fun fun36() {}

context(=) fun fun37() {}
context(:) fun fun38() {}
context(,:) fun fun39() {}
context(:,) fun fun40() {}
context(=,) fun fun41() {}
context(,=) fun fun42() {}
context(:=) fun fun43() {}
context(=:) fun fun44() {}
context(:,=) fun fun45() {}
context(=,:) fun fun46() {}
context(=, ,:) fun fun47() {}
context(, :, =,) fun fun48() {}

// actually `val`/`var` don't get parsed inside context parameter list
context(val) fun fun49() {}
context(val ,) fun fun50() {}
context(var , var) fun fun51() {}
