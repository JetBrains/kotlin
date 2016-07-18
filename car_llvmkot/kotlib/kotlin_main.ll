declare void @llvm.memcpy.p0i8.p0i8.i64(i8* nocapture, i8* nocapture readonly, i64, i32, i1)
declare i8* @malloc_static(i32)
attributes #0 = { nounwind "stack-protector-buffer-size"="8" "target-cpu"="cortex-m3" "target-features"="+hwdiv,+strict-align" }
%class.Simple = type { i32 }
define void @Simple(%class.Simple*  %classvariable.this, i32  %i) #0
{
%classvariable.this.addr = alloca %class.Simple, align 4
%i.addr = alloca i32, align 4
store i32 %i, i32* %i.addr, align 4
%var1 = load i32* %i.addr, align 4
%var2 = getelementptr inbounds %class.Simple* %classvariable.this.addr, i32 0, i32 0
store i32 %var1, i32* %var2, align 4
%var3 = bitcast %class.Simple* %classvariable.this to i8*
%var4 = bitcast %class.Simple* %classvariable.this.addr to i8*
call void @llvm.memcpy.p0i8.p0i8.i64(i8* %var3, i8* %var4, i64 4, i32 4, i1 false)
ret void 
}
define void @a() #0
{
ret void 
}
define void @kotlin_main() #0
{
%var6 = call i8* @malloc_static(i32 4)
%var5 = bitcast i8* %var6 to %class.Simple*
call void @Simple(%class.Simple* %var5, i32 5)
%managed.s.1 = alloca %class.Simple, align 4
%var7 = load %class.Simple* %var5, align 4
store %class.Simple %var7, %class.Simple* %managed.s.1, align 4
call void @a()
ret void 
}

