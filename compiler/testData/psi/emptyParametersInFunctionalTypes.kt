// COMPILATION_ERRORS

val val1: () -> Unit
val val2: (,) -> Unit
val val3: (normal: Type,) -> Unit
val val4: (, normal: Type) -> Unit
val val5: (normal: Type, ,) -> Unit
val val6: (normal: Type, , normal: Type) -> Unit
val val7: (normal: Type, , normal: Type,) -> Unit
val val8: (, , normal: Type) -> Unit

val val9: (justName,) -> Unit
val val10: (justName, ,) -> Unit
val val11: (normal: Type, , justName) -> Unit
val val12: (, justName,) -> Unit
val val13: (justName, , justName) -> Unit

val val14: ( ,val property: Type,) -> Unit
val val15: (var property, ,) -> Unit

val val16: (, @Anno justName) -> Unit
val val17: (@DanglingAnno , @Anno normal: Type) -> Unit
val val18: (@DanglingAnno, @DanglingAnno) -> Unit
val val19: (@Anno justName, , normal: Type) -> Unit

val val20: (vararg , vararg justName, vararg normal: Type,) -> Unit
val val21: (vararg , ,) -> Unit
val val22: (, vararg justName) -> Unit

val val23: (: JustType) -> Unit
val val24: (: JustType normal: Type) -> Unit
val val25: (, : JustType) -> Unit
val val26: (: JustType, , justName: ) -> Unit

val val27: (normal: Type =, ) -> Unit
val val28: (normal: Type = default, ,) -> Unit
val val29: (normal: Type = @DanglingAnno) -> Unit
val val30: (normal: Type = @DanglingAnno, ,) -> Unit

val val31: (normal: @DanglingAnno) -> Unit
val val32: (normal: @DanglingAnno, ,) -> Unit
val val33: (normal: @DanglingAnno val property: @DanglingAnno =) -> Unit

val val34: (,=, ,) -> Unit
val val35: (= default, ,) -> Unit
val val36: (, : JustType = default, ,) -> Unit

val val37: (=) -> Unit
val val38: (:) -> Unit
val val39: (,:) -> Unit
val val40: (:,) -> Unit
val val41: (=,) -> Unit
val val42: (,=) -> Unit
val val43: (:=) -> Unit
val val44: (=:) -> Unit
val val45: (:,=) -> Unit
val val46: (=,:) -> Unit
val val47: (=, ,:) -> Unit
val val48: (, :, =,) -> Unit

val val49: (val) -> Unit
val val50: (val ,) -> Unit
val val51: (var , var) -> Unit
