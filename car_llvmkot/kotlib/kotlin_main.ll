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
%class.ByteArray = type { i32, i32 }
define void @ByteArray(%class.ByteArray*  %classvariable.this, i32  %size) 
{
%classvariable.this.addr = alloca %class.ByteArray, align 4
%size.addr = alloca i32, align 4
store i32 %size, i32* %size.addr, align 4
%var5 = load i32* %size.addr, align 4
%var6 = getelementptr inbounds %class.ByteArray* %classvariable.this.addr, i32 0, i32 0
store i32 %var5, i32* %var6, align 4
%var7 = bitcast %class.ByteArray* %classvariable.this to i8*
%var8 = bitcast %class.ByteArray* %classvariable.this.addr to i8*
call void @llvm.memcpy.p0i8.p0i8.i64(i8* %var7, i8* %var8, i64 8, i32 4, i1 false)
%var9 = getelementptr inbounds %class.ByteArray* %classvariable.this, i32 0, i32 1
%var10 = getelementptr inbounds %class.ByteArray* %classvariable.this, i32 0, i32 0
%var11 = load i32* %var10, align 4
%var12 = call i32 @malloc_array(i32 %var11)
%var13 = alloca i32, align 4
store i32 %var12, i32* %var13, align 4
%var14 = load i32* %var9, align 4
%var15 = load i32* %var13, align 4
store i32 %var15, i32* %var9, align 4
ret void 
}
define i8 @ByteArray.get(%class.ByteArray*  %classvariable.this, i32  %index) 
{
%index.addr = alloca i32, align 4
store i32 %index, i32* %index.addr, align 4
%var16 = getelementptr inbounds %class.ByteArray* %classvariable.this, i32 0, i32 1
%var17 = load i32* %var16, align 4
%var18 = load i32* %index.addr, align 4
%var19 = call i8 @kotlinclib_get_byte(i32 %var17, i32 %var18)
%var20 = alloca i8, align 1
store i8 %var19, i8* %var20, align 1
%var21 = load i8* %var20, align 1
ret i8 %var21
}
define void @ByteArray.clone(%class.ByteArray**  %instance, %class.ByteArray*  %classvariable.this) 
{
%var22 = getelementptr inbounds %class.ByteArray* %classvariable.this, i32 0, i32 0
%var23 = load i32* %var22, align 4
%var25 = call i8* @malloc(i32 8)
%var24 = bitcast i8* %var25 to %class.ByteArray*
call void @ByteArray(%class.ByteArray* %var24, i32 %var23)
%managed.index.1 = alloca i32, align 4
store i32 0, i32* %managed.index.1, align 4
br label %label.while.1
label.while.1:
%var26 = getelementptr inbounds %class.ByteArray* %classvariable.this, i32 0, i32 0
%var27 = load i32* %managed.index.1, align 4
%var28 = load i32* %var26, align 4
%var29 = icmp slt i32 %var27, %var28
br i1 %var29, label %label.while.2, label %label.while.3
label.while.2:
%var30 = load i32* %managed.index.1, align 4
%var31 = call i8 @ByteArray.get(%class.ByteArray* %classvariable.this, i32 %var30)
%var32 = alloca i8, align 1
store i8 %var31, i8* %var32, align 1
%var33 = load i32* %managed.index.1, align 4
%var34 = load i8* %var32, align 1
call void @ByteArray.set(%class.ByteArray* %var24, i32 %var33, i8 %var34)
%var35 = load i32* %managed.index.1, align 4
%var36 = add nsw i32 %var35, 1
%var37 = load i32* %managed.index.1, align 4
store i32 %var36, i32* %managed.index.1, align 4
br label %label.while.1
label.while.3:
store %class.ByteArray* %var24, %class.ByteArray** %instance, align 4
ret void 
}
define void @ByteArray.set(%class.ByteArray*  %classvariable.this, i32  %index, i8  %value) 
{
%index.addr = alloca i32, align 4
store i32 %index, i32* %index.addr, align 4
%value.addr = alloca i8, align 1
store i8 %value, i8* %value.addr, align 1
%var38 = getelementptr inbounds %class.ByteArray* %classvariable.this, i32 0, i32 1
%var39 = load i32* %var38, align 4
%var40 = load i32* %index.addr, align 4
%var41 = load i8* %value.addr, align 1
call void @kotlinclib_set_byte(i32 %var39, i32 %var40, i8 %var41)
ret void 
}
declare i8 @kotlinclib_get_byte(i32  %src, i32  %index) 
declare i32 @malloc_array(i32  %size) 
declare void @kotlinclib_set_byte(i32  %src, i32  %index, i8  %value) 
define void @createMyClass(%class.MyClass**  %instance) 
{
%var43 = call i8* @malloc(i32 4)
%var42 = bitcast i8* %var43 to %class.MyClass*
call void @MyClass(%class.MyClass* %var42, i32 1)
store %class.MyClass* %var42, %class.MyClass** %instance, align 4
ret void 
}

