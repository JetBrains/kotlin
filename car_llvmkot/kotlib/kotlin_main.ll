declare void @llvm.memcpy.p0i8.p0i8.i64(i8* nocapture, i8* nocapture readonly, i64, i32, i1)
declare i8* @malloc_static(i32)
attributes #0 = { nounwind "stack-protector-buffer-size"="8" "target-cpu"="cortex-m3" "target-features"="+hwdiv,+strict-align" }
define i32 @reassigment_1(i32  %x) #0
{
%x.addr = alloca i32, align 4
store i32 %x, i32* %x.addr, align 4
%var1 = load i32* %x.addr, align 4
%var2 = add nsw i32 %var1, 1
%var3 = add nsw i32 %var2, 1
store i32 %var3, i32 %var2, align 4
ret i32 %var2
}

