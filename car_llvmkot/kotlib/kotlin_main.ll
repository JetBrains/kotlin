declare void @llvm.memcpy.p0i8.p0i8.i64(i8* nocapture, i8* nocapture readonly, i64, i32, i1)
attributes #0 = { nounwind "stack-protector-buffer-size"="8" "target-cpu"="cortex-m3" "target-features"="+hwdiv,+strict-align" }
@CAR_MODE_REMOTE_CONTROL = global i32 1, align 4
@PROGRAM_DURATIOH_MS = global i32 3000, align 4
@CAR_MODE_LAST = global i32 2, align 4
@CAR_MODE_PROGRAMMED = global i32 0, align 4
%class.MyClass = type { i32, i16, i8 }
define void @MyClass(%class.MyClass*  %instance, i32  %i, i16  %j, i8  %k) #0
{
%instance.addr = alloca %class.MyClass, align 4
%i.addr = alloca i32, align 4
store i32 %i, i32* %i.addr, align 4
%j.addr = alloca i16, align 2
store i16 %j, i16* %j.addr, align 2
%k.addr = alloca i8, align 1
store i8 %k, i8* %k.addr, align 1
%var1 = load i32* %i.addr, align 4
%var2 = getelementptr inbounds %class.MyClass* %instance.addr, i32 0, i32 0
store i32 %var1, i32* %var2, align 4
%var3 = load i16* %j.addr, align 2
%var4 = getelementptr inbounds %class.MyClass* %instance.addr, i32 0, i32 1
store i16 %var3, i16* %var4, align 2
%var5 = load i8* %k.addr, align 1
%var6 = getelementptr inbounds %class.MyClass* %instance.addr, i32 0, i32 2
store i8 %var5, i8* %var6, align 1
%var7 = bitcast %class.MyClass* %instance to i8*
%var8 = bitcast %class.MyClass* %instance.addr to i8*
call void @llvm.memcpy.p0i8.p0i8.i64(i8* %var7, i8* %var8, i64 7, i32 4, i1 false)
ret void 
}
declare void @wait(i32  %i) #0
define void @engine_program() #0
{
call void @engine_init()
br label %label.while.1
label.while.1:
%var9 = icmp slt i32 2, 3
br i1 %var9, label %label.while.2, label %label.while.3
label.while.2:
call void @engine_forward()
%var10 = load i32* @PROGRAM_DURATIOH_MS, align 4
call void @wait(i32 %var10)
call void @engine_stop()
call void @engine_backward()
%var11 = load i32* @PROGRAM_DURATIOH_MS, align 4
call void @wait(i32 %var11)
call void @engine_stop()
call void @engine_turn_right()
%var12 = load i32* @PROGRAM_DURATIOH_MS, align 4
call void @wait(i32 %var12)
call void @engine_stop()
call void @engine_turn_right()
%var13 = load i32* @PROGRAM_DURATIOH_MS, align 4
call void @wait(i32 %var13)
call void @engine_stop()
br label %label.while.1
label.while.3:
ret void 
}
declare void @time_init() #0
declare void @leds_init() #0
declare void @engine_init() #0
declare void @run_rc_car(void ()*  %i) #0
declare void @engine_backward() #0
define void @kotlin_main() #0
{
call void @time_init()
call void @engine_program()
ret void 
}
declare void @VCP_init() #0
declare void @engine_stop() #0
declare void @user_brn_init(void ()*  %i) #0
declare void @engine_forward() #0
declare void @run_programmed_car(void ()*  %i) #0
declare void @engine_turn_left() #0
declare void @engine_turn_right() #0

