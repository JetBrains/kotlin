declare void @llvm.memcpy.p0i8.p0i8.i64(i8* nocapture, i8* nocapture readonly, i64, i32, i1)
attributes #0 = { nounwind "stack-protector-buffer-size"="8" "target-cpu"="cortex-m3" "target-features"="+hwdiv,+strict-align" }
declare void @wait(i32 %loops) #0
define void @program_forward() #0
{
call void @engine_forward()
call void @wait(i32 10)
ret void
}
declare void @leds_init() #0
declare void @engine_init() #0
define void @program_backward() #0
{
call void @engine_backward()
call void @wait(i32 10)
ret void
}
declare void @engine_backward() #0
define void @program_rotation_left() #0
{
call void @engine_turn_left()
call void @wait(i32 10)
ret void
}
define void @kotlin_main() #0
{
call void @leds_init()
call void @engine_init()
ret void
}
declare void @engine_stop() #0
declare void @engine_forward() #0
define void @program_rotation_right() #0
{
call void @engine_turn_right()
call void @wait(i32 10)
ret void
}
declare void @engine_turn_left() #0
declare void @engine_turn_right() #0

