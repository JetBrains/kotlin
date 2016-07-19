declare void @llvm.memcpy.p0i8.p0i8.i64(i8* nocapture, i8* nocapture readonly, i64, i32, i1)
declare i8* @malloc(i32)
%class.MyClass = type { i32 }
define void @MyClass(%class.MyClass*  %classvariable.this, i32  %i) 
{
%classvariable.this.addr = alloca %class.MyClass, align 4
%i.addr = alloca i32, align 4
store i32 %i, i32* %i.addr, align 4
%var1 = load i32* %i.addr, align 4
%var2 = getelementptr inbounds %class.MyClass* %classvariable.this.addr, i32 0, i32 0
store i32 %var1, i32* %var2, align 4
%var3 = bitcast %class.MyClass* %classvariable.this to i8*
%var4 = bitcast %class.MyClass* %classvariable.this.addr to i8*
call void @llvm.memcpy.p0i8.p0i8.i64(i8* %var3, i8* %var4, i64 4, i32 4, i1 false)
ret void 
}
define void @main() 
{
%a = alloca %class.MyClass*, align 4
store %class.MyClass* null, %class.MyClass** %a, align 4
%var5 = load %class.MyClass** %a, align 4
%var7 = call i8* @malloc(i32 4)
%var6 = bitcast i8* %var7 to %class.MyClass*
call void @MyClass(%class.MyClass* %var6, i32 2)
%var8 = load %class.MyClass* %var5, align 4
%var9 = load %class.MyClass* %var6, align 4
ret void 
}

