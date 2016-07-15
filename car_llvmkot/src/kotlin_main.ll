declare void @llvm.memcpy.p0i8.p0i8.i64(i8* nocapture, i8* nocapture readonly, i64, i32, i1)
attributes #0 = { nounwind "stack-protector-buffer-size"="8" "target-cpu"="cortex-m3" "target-features"="+hwdiv,+strict-align" }
@CAR_MODE_REMOTE_CONTROL = global i32 1, align 4
@CAR_MODE_LAST = global i32 2, align 4
@CAR_MODE_PROGRAMMED = global i32 0, align 4
declare i32 @get_mode() #0
declare void @start_current_mode() #0
declare void @time_init() #0
declare void @leds_init() #0
declare void @engine_init() #0
declare void @run_rc_car(void ()*  %i) #0
declare void @engine_backward() #0
define i32 @kotlin_main() #0
{
%managed.i.1 = alloca i32, align 4
store i32 0, i32* %managed.i.1, align 4
%managed.mode.1 = alloca i32, align 4
store i32 0, i32* %managed.mode.1, align 4
br label %label.while.1
label.while.1:
%var1 = icmp slt i32 2, 3
br i1 %var1, label %label.while.2, label %label.while.3
label.while.2:
%var2 = load i32* %managed.mode.1, align 4
%var3 = load i32* @CAR_MODE_PROGRAMMED, align 4
%var4 = icmp eq i32 %var2, %var3
br i1 %var4, label %label.if.4, label %label.if.5
label.if.4:
call void @run_programmed_car(void ()* @stop_current_mode)
br label %label.if.6
label.if.5:
%var5 = load i32* %managed.i.1, align 4
store i32 1, i32* %managed.i.1, align 4
br label %label.if.6
label.if.6:
br label %label.while.1
label.while.3:
ret i32 0
}
declare void @stop_current_mode() #0
declare void @VCP_init() #0
declare void @engine_stop() #0
declare void @user_brn_init(void ()*  %i) #0
declare void @set_mode(i32  %mode) #0
declare void @engine_forward() #0
declare void @run_programmed_car(void ()*  %i) #0
declare void @engine_turn_left() #0
declare void @engine_turn_right() #0

