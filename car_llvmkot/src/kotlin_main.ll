declare void @llvm.memcpy.p0i8.p0i8.i64(i8* nocapture, i8* nocapture readonly, i64, i32, i1)
attributes #0 = { nounwind "stack-protector-buffer-size"="8" "target-cpu"="cortex-m3" "target-features"="+hwdiv,+strict-align" }
declare void @wait(i32 %loops) #0
define void @programRotationRight() #0
{
call void @engine_turn_right()
call void @wait(i32 10)
ret void
}
define void @setCurProgram(i32 %program) #0
{
%program.addr = alloca i32, align 4
store i32 %program, i32* %program.addr, align 4
ret void
}
define void @programBackward() #0
{
call void @engine_backward()
call void @wait(i32 10)
ret void
}
declare void @leds_init() #0
define void @programForward(i32 %delay) #0
{
%delay.addr = alloca i32, align 4
store i32 %delay, i32* %delay.addr, align 4
call void @engine_forward()
call void @wait(i32 10)
ret void
}
define void @procNextProgramPending() #0
{
ret void
}
declare void @engine_init() #0
declare void @user_btn_init(void ()* %program) #0
define void @setNextProgramPending() #0
{
ret void
}
declare void @engine_backward() #0
define void @programRotationLeft() #0
{
call void @engine_turn_left()
call void @wait(i32 10)
ret void
}
define void @kotlin_main() #0
{
call void @leds_init()
call void @engine_init()
call void @user_btn_init(void ()* @setNextProgramPending)
call void @setCurProgram(i32 0)
ret void
}
declare void @engine_stop() #0
declare void @engine_forward() #0
declare void @engine_turn_left() #0
declare void @engine_turn_right() #0

