; ModuleID = 'memory.c'
target datalayout = "e-m:e-p:32:32-i64:64-v128:64:128-a:0:32-n32-S64"
target triple = "thumbv7m-none--eabi"

@static_area_ptr = global i32 0, align 4
@dynamic_area_ptr = global i32 0, align 4
@static_area = common global [1000 x i8] zeroinitializer, align 1
@dynamic_area = common global [1000 x i8] zeroinitializer, align 1

; Function Attrs: nounwind
define void @init_dynamic_area() #0 {
  store i32 0, i32* @dynamic_area_ptr, align 4
  ret void
}

; Function Attrs: nounwind
define i8* @malloc_static(i32 %size) #0 {
  %1 = alloca i8*, align 4
  %2 = alloca i32, align 4
  %result = alloca i32, align 4
  store i32 %size, i32* %2, align 4
  %3 = load i32* %2, align 4
  %4 = load i32* @static_area_ptr, align 4
  %5 = add nsw i32 %3, %4
  %6 = icmp sgt i32 %5, 1000
  br i1 %6, label %7, label %8

; <label>:7                                       ; preds = %0
  store i8* null, i8** %1
  br label %15

; <label>:8                                       ; preds = %0
  %9 = load i32* @static_area_ptr, align 4
  store i32 %9, i32* %result, align 4
  %10 = load i32* %2, align 4
  %11 = load i32* @static_area_ptr, align 4
  %12 = add nsw i32 %11, %10
  store i32 %12, i32* @static_area_ptr, align 4
  %13 = load i32* %result, align 4
  %14 = inttoptr i32 %13 to i8*
  store i8* %14, i8** %1
  br label %15

; <label>:15                                      ; preds = %8, %7
  %16 = load i8** %1
  ret i8* %16
}

; Function Attrs: nounwind
define i8* @malloc_dynamic(i32 %size) #0 {
  %1 = alloca i8*, align 4
  %2 = alloca i32, align 4
  %result = alloca i32, align 4
  store i32 %size, i32* %2, align 4
  %3 = load i32* %2, align 4
  %4 = load i32* @dynamic_area_ptr, align 4
  %5 = add nsw i32 %3, %4
  %6 = icmp sgt i32 %5, 1000
  br i1 %6, label %7, label %8

; <label>:7                                       ; preds = %0
  store i8* null, i8** %1
  br label %15

; <label>:8                                       ; preds = %0
  %9 = load i32* @dynamic_area_ptr, align 4
  store i32 %9, i32* %result, align 4
  %10 = load i32* %2, align 4
  %11 = load i32* @dynamic_area_ptr, align 4
  %12 = add nsw i32 %11, %10
  store i32 %12, i32* @dynamic_area_ptr, align 4
  %13 = load i32* %result, align 4
  %14 = inttoptr i32 %13 to i8*
  store i8* %14, i8** %1
  br label %15

; <label>:15                                      ; preds = %8, %7
  %16 = load i8** %1
  ret i8* %16
}

attributes #0 = { nounwind "less-precise-fpmad"="false" "no-frame-pointer-elim"="true" "no-frame-pointer-elim-non-leaf" "no-infs-fp-math"="false" "no-nans-fp-math"="false" "stack-protector-buffer-size"="8" "unsafe-fp-math"="false" "use-soft-float"="false" }

!llvm.module.flags = !{!0, !1}
!llvm.ident = !{!2}

!0 = !{i32 1, !"wchar_size", i32 4}
!1 = !{i32 1, !"min_enum_size", i32 4}
!2 = !{!"Ubuntu clang version 3.6.2-3ubuntu2 (tags/RELEASE_362/final) (based on LLVM 3.6.2)"}
