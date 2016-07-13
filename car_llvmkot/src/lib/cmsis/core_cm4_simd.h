/**************************************************************************//**
 * @file     core_cm4_simd.h
 * @brief    CMSIS Cortex-M4 SIMD Header File
 * @version  V2.10
 * @date     19. July 2011
 *
 * @note
 * Copyright (C) 2010-2011 ARM Limited. All rights reserved.
 *
 * @par
 * ARM Limited (ARM) is supplying this software for use with Cortex-M 
 * processor based microcontrollers.  This file can be freely distributed 
 * within development tools that are supporting such ARM based processors. 
 *
 * @par
 * THIS SOFTWARE IS PROVIDED "AS IS".  NO WARRANTIES, WHETHER EXPRESS, IMPLIED
 * OR STATUTORY, INCLUDING, BUT NOT LIMITED TO, IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE APPLY TO THIS SOFTWARE.
 * ARM SHALL NOT, IN ANY CIRCUMSTANCES, BE LIABLE FOR SPECIAL, INCIDENTAL, OR
 * CONSEQUENTIAL DAMAGES, FOR ANY REASON WHATSOEVER.
 *
 ******************************************************************************/

#ifdef __cplusplus
 extern "C" {
#endif 

#ifndef __CORE_CM4_SIMD_H
#define __CORE_CM4_SIMD_H


/*******************************************************************************
 *                Hardware Abstraction Layer
 ******************************************************************************/


/* ###################  Compiler specific Intrinsics  ########################### */
/** \defgroup CMSIS_SIMD_intrinsics CMSIS SIMD Intrinsics
  Access to dedicated SIMD instructions
  @{
*/

#if   defined ( __CC_ARM ) /*------------------RealView Compiler -----------------*/
/* ARM armcc specific functions */

/*------ CM4 SOMD Intrinsics -----------------------------------------------------*/
#define __SADD8                           __sadd8
#define __QADD8                           __qadd8
#define __SHADD8                          __shadd8
#define __UADD8                           __uadd8
#define __UQADD8                          __uqadd8
#define __UHADD8                          __uhadd8
#define __SSUB8                           __ssub8
#define __QSUB8                           __qsub8
#define __SHSUB8                          __shsub8
#define __USUB8                           __usub8
#define __UQSUB8                          __uqsub8
#define __UHSUB8                          __uhsub8
#define __SADD16                          __sadd16
#define __QADD16                          __qadd16
#define __SHADD16                         __shadd16
#define __UADD16                          __uadd16
#define __UQADD16                         __uqadd16
#define __UHADD16                         __uhadd16
#define __SSUB16                          __ssub16
#define __QSUB16                          __qsub16
#define __SHSUB16                         __shsub16
#define __USUB16                          __usub16
#define __UQSUB16                         __uqsub16
#define __UHSUB16                         __uhsub16
#define __SASX                            __sasx
#define __QASX                            __qasx
#define __SHASX                           __shasx
#define __UASX                            __uasx
#define __UQASX                           __uqasx
#define __UHASX                           __uhasx
#define __SSAX                            __ssax
#define __QSAX                            __qsax
#define __SHSAX                           __shsax
#define __USAX                            __usax
#define __UQSAX                           __uqsax
#define __UHSAX                           __uhsax
#define __USAD8                           __usad8
#define __USADA8                          __usada8
#define __SSAT16                          __ssat16
#define __USAT16                          __usat16
#define __UXTB16                          __uxtb16
#define __UXTAB16                         __uxtab16
#define __SXTB16                          __sxtb16
#define __SXTAB16                         __sxtab16
#define __SMUAD                           __smuad
#define __SMUADX                          __smuadx
#define __SMLAD                           __smlad
#define __SMLADX                          __smladx
#define __SMLALD                          __smlald
#define __SMLALDX                         __smlaldx
#define __SMUSD                           __smusd
#define __SMUSDX                          __smusdx
#define __SMLSD                           __smlsd
#define __SMLSDX                          __smlsdx
#define __SMLSLD                          __smlsld
#define __SMLSLDX                         __smlsldx
#define __SEL                             __sel
#define __QADD                            __qadd
#define __QSUB                            __qsub

#define __PKHBT(ARG1,ARG2,ARG3)          ( ((((uint32_t)(ARG1))          ) & 0x0000FFFFUL) |  \
                                           ((((uint32_t)(ARG2)) << (ARG3)) & 0xFFFF0000UL)  )

#define __PKHTB(ARG1,ARG2,ARG3)          ( ((((uint32_t)(ARG1))          ) & 0xFFFF0000UL) |  \
                                           ((((uint32_t)(ARG2)) >> (ARG3)) & 0x0000FFFFUL)  )


/*-- End CM4 SIMD Intrinsics -----------------------------------------------------*/



#elif defined ( __ICCARM__ ) /*------------------ ICC Compiler -------------------*/
/* IAR iccarm specific functions */

#include <cmsis_iar.h>

/*------ CM4 SIMDDSP Intrinsics -----------------------------------------------------*/
/* intrinsic __SADD8      see intrinsics.h */
/* intrinsic __QADD8      see intrinsics.h */
/* intrinsic __SHADD8     see intrinsics.h */
/* intrinsic __UADD8      see intrinsics.h */
/* intrinsic __UQADD8     see intrinsics.h */
/* intrinsic __UHADD8     see intrinsics.h */
/* intrinsic __SSUB8      see intrinsics.h */
/* intrinsic __QSUB8      see intrinsics.h */
/* intrinsic __SHSUB8     see intrinsics.h */
/* intrinsic __USUB8      see intrinsics.h */
/* intrinsic __UQSUB8     see intrinsics.h */
/* intrinsic __UHSUB8     see intrinsics.h */
/* intrinsic __SADD16     see intrinsics.h */
/* intrinsic __QADD16     see intrinsics.h */
/* intrinsic __SHADD16    see intrinsics.h */
/* intrinsic __UADD16     see intrinsics.h */
/* intrinsic __UQADD16    see intrinsics.h */
/* intrinsic __UHADD16    see intrinsics.h */
/* intrinsic __SSUB16     see intrinsics.h */
/* intrinsic __QSUB16     see intrinsics.h */
/* intrinsic __SHSUB16    see intrinsics.h */
/* intrinsic __USUB16     see intrinsics.h */
/* intrinsic __UQSUB16    see intrinsics.h */
/* intrinsic __UHSUB16    see intrinsics.h */
/* intrinsic __SASX       see intrinsics.h */
/* intrinsic __QASX       see intrinsics.h */
/* intrinsic __SHASX      see intrinsics.h */
/* intrinsic __UASX       see intrinsics.h */
/* intrinsic __UQASX      see intrinsics.h */
/* intrinsic __UHASX      see intrinsics.h */
/* intrinsic __SSAX       see intrinsics.h */
/* intrinsic __QSAX       see intrinsics.h */
/* intrinsic __SHSAX      see intrinsics.h */
/* intrinsic __USAX       see intrinsics.h */
/* intrinsic __UQSAX      see intrinsics.h */
/* intrinsic __UHSAX      see intrinsics.h */
/* intrinsic __USAD8      see intrinsics.h */
/* intrinsic __USADA8     see intrinsics.h */
/* intrinsic __SSAT16     see intrinsics.h */
/* intrinsic __USAT16     see intrinsics.h */
/* intrinsic __UXTB16     see intrinsics.h */
/* intrinsic __SXTB16     see intrinsics.h */
/* intrinsic __UXTAB16    see intrinsics.h */
/* intrinsic __SXTAB16    see intrinsics.h */
/* intrinsic __SMUAD      see intrinsics.h */
/* intrinsic __SMUADX     see intrinsics.h */
/* intrinsic __SMLAD      see intrinsics.h */
/* intrinsic __SMLADX     see intrinsics.h */
/* intrinsic __SMLALD     see intrinsics.h */
/* intrinsic __SMLALDX    see intrinsics.h */
/* intrinsic __SMUSD      see intrinsics.h */
/* intrinsic __SMUSDX     see intrinsics.h */
/* intrinsic __SMLSD      see intrinsics.h */
/* intrinsic __SMLSDX     see intrinsics.h */
/* intrinsic __SMLSLD     see intrinsics.h */
/* intrinsic __SMLSLDX    see intrinsics.h */
/* intrinsic __SEL        see intrinsics.h */
/* intrinsic __QADD       see intrinsics.h */
/* intrinsic __QSUB       see intrinsics.h */
/* intrinsic __PKHBT      see intrinsics.h */
/* intrinsic __PKHTB      see intrinsics.h */

/*-- End CM4 SIMD Intrinsics -----------------------------------------------------*/



#elif defined ( __GNUC__ ) /*------------------ GNU Compiler ---------------------*/
/* GNU gcc specific functions */

/*------ CM4 SIMD Intrinsics -----------------------------------------------------*/
__attribute__( ( always_inline ) ) static __INLINE uint32_t __SADD8(uint32_t op1, uint32_t op2)
{
  uint32_t result;
  
  __ASM volatile ("sadd8 %0, %1, %2" : "=r" (result) : "r" (op1), "r" (op2) );
  return(result);
}

__attribute__( ( always_inline ) ) static __INLINE uint32_t __QADD8(uint32_t op1, uint32_t op2)
{
  uint32_t result;
  
  __ASM volatile ("qadd8 %0, %1, %2" : "=r" (result) : "r" (op1), "r" (op2) );
  return(result);
}

__attribute__( ( always_inline ) ) static __INLINE uint32_t __SHADD8(uint32_t op1, uint32_t op2)
{
  uint32_t result;
  
  __ASM volatile ("shadd8 %0, %1, %2" : "=r" (result) : "r" (op1), "r" (op2) );
  return(result);
}

__attribute__( ( always_inline ) ) static __INLINE uint32_t __UADD8(uint32_t op1, uint32_t op2)
{
  uint32_t result;
  
  __ASM volatile ("uadd8 %0, %1, %2" : "=r" (result) : "r" (op1), "r" (op2) );
  return(result);
}

__attribute__( ( always_inline ) ) static __INLINE uint32_t __UQADD8(uint32_t op1, uint32_t op2)
{
  uint32_t result;
  
  __ASM volatile ("uqadd8 %0, %1, %2" : "=r" (result) : "r" (op1), "r" (op2) );
  return(result);
}

__attribute__( ( always_inline ) ) static __INLINE uint32_t __UHADD8(uint32_t op1, uint32_t op2)
{
  uint32_t result;
  
  __ASM volatile ("uhadd8 %0, %1, %2" : "=r" (result) : "r" (op1), "r" (op2) );
  return(result);
}


__attribute__( ( always_inline ) ) static __INLINE uint32_t __SSUB8(uint32_t op1, uint32_t op2)
{
  uint32_t result;
  
  __ASM volatile ("ssub8 %0, %1, %2" : "=r" (result) : "r" (op1), "r" (op2) );
  return(result);
}

__attribute__( ( always_inline ) ) static __INLINE uint32_t __QSUB8(uint32_t op1, uint32_t op2)
{
  uint32_t result;
  
  __ASM volatile ("qsub8 %0, %1, %2" : "=r" (result) : "r" (op1), "r" (op2) );
  return(result);
}

__attribute__( ( always_inline ) ) static __INLINE uint32_t __SHSUB8(uint32_t op1, uint32_t op2)
{
  uint32_t result;
  
  __ASM volatile ("shsub8 %0, %1, %2" : "=r" (result) : "r" (op1), "r" (op2) );
  return(result);
}

__attribute__( ( always_inline ) ) static __INLINE uint32_t __USUB8(uint32_t op1, uint32_t op2)
{
  uint32_t result;
  
  __ASM volatile ("usub8 %0, %1, %2" : "=r" (result) : "r" (op1), "r" (op2) );
  return(result);
}

__attribute__( ( always_inline ) ) static __INLINE uint32_t __UQSUB8(uint32_t op1, uint32_t op2)
{
  uint32_t result;
  
  __ASM volatile ("uqsub8 %0, %1, %2" : "=r" (result) : "r" (op1), "r" (op2) );
  return(result);
}

__attribute__( ( always_inline ) ) static __INLINE uint32_t __UHSUB8(uint32_t op1, uint32_t op2)
{
  uint32_t result;
  
  __ASM volatile ("uhsub8 %0, %1, %2" : "=r" (result) : "r" (op1), "r" (op2) );
  return(result);
}


__attribute__( ( always_inline ) ) static __INLINE uint32_t __SADD16(uint32_t op1, uint32_t op2)
{
  uint32_t result;
  
  __ASM volatile ("sadd16 %0, %1, %2" : "=r" (result) : "r" (op1), "r" (op2) );
  return(result);
}

__attribute__( ( always_inline ) ) static __INLINE uint32_t __QADD16(uint32_t op1, uint32_t op2)
{
  uint32_t result;
  
  __ASM volatile ("qadd16 %0, %1, %2" : "=r" (result) : "r" (op1), "r" (op2) );
  return(result);
}

__attribute__( ( always_inline ) ) static __INLINE uint32_t __SHADD16(uint32_t op1, uint32_t op2)
{
  uint32_t result;
  
  __ASM volatile ("shadd16 %0, %1, %2" : "=r" (result) : "r" (op1), "r" (op2) );
  return(result);
}

__attribute__( ( always_inline ) ) static __INLINE uint32_t __UADD16(uint32_t op1, uint32_t op2)
{
  uint32_t result;
  
  __ASM volatile ("uadd16 %0, %1, %2" : "=r" (result) : "r" (op1), "r" (op2) );
  return(result);
}

__attribute__( ( always_inline ) ) static __INLINE uint32_t __UQADD16(uint32_t op1, uint32_t op2)
{
  uint32_t result;
  
  __ASM volatile ("uqadd16 %0, %1, %2" : "=r" (result) : "r" (op1), "r" (op2) );
  return(result);
}

__attribute__( ( always_inline ) ) static __INLINE uint32_t __UHADD16(uint32_t op1, uint32_t op2)
{
  uint32_t result;
  
  __ASM volatile ("uhadd16 %0, %1, %2" : "=r" (result) : "r" (op1), "r" (op2) );
  return(result);
}

__attribute__( ( always_inline ) ) static __INLINE uint32_t __SSUB16(uint32_t op1, uint32_t op2)
{
  uint32_t result;
  
  __ASM volatile ("ssub16 %0, %1, %2" : "=r" (result) : "r" (op1), "r" (op2) );
  return(result);
}

__attribute__( ( always_inline ) ) static __INLINE uint32_t __QSUB16(uint32_t op1, uint32_t op2)
{
  uint32_t result;
  
  __ASM volatile ("qsub16 %0, %1, %2" : "=r" (result) : "r" (op1), "r" (op2) );
  return(result);
}

__attribute__( ( always_inline ) ) static __INLINE uint32_t __SHSUB16(uint32_t op1, uint32_t op2)
{
  uint32_t result;
  
  __ASM volatile ("shsub16 %0, %1, %2" : "=r" (result) : "r" (op1), "r" (op2) );
  return(result);
}

__attribute__( ( always_inline ) ) static __INLINE uint32_t __USUB16(uint32_t op1, uint32_t op2)
{
  uint32_t result;
  
  __ASM volatile ("usub16 %0, %1, %2" : "=r" (result) : "r" (op1), "r" (op2) );
  return(result);
}

__attribute__( ( always_inline ) ) static __INLINE uint32_t __UQSUB16(uint32_t op1, uint32_t op2)
{
  uint32_t result;
  
  __ASM volatile ("uqsub16 %0, %1, %2" : "=r" (result) : "r" (op1), "r" (op2) );
  return(result);
}

__attribute__( ( always_inline ) ) static __INLINE uint32_t __UHSUB16(uint32_t op1, uint32_t op2)
{
  uint32_t result;
  
  __ASM volatile ("uhsub16 %0, %1, %2" : "=r" (result) : "r" (op1), "r" (op2) );
  return(result);
}

__attribute__( ( always_inline ) ) static __INLINE uint32_t __SASX(uint32_t op1, uint32_t op2)
{
  uint32_t result;
  
  __ASM volatile ("sasx %0, %1, %2" : "=r" (result) : "r" (op1), "r" (op2) );
  return(result);
}

__attribute__( ( always_inline ) ) static __INLINE uint32_t __QASX(uint32_t op1, uint32_t op2)
{
  uint32_t result;
  
  __ASM volatile ("qasx %0, %1, %2" : "=r" (result) : "r" (op1), "r" (op2) );
  return(result);
}

__attribute__( ( always_inline ) ) static __INLINE uint32_t __SHASX(uint32_t op1, uint32_t op2)
{
  uint32_t result;
  
  __ASM volatile ("shasx %0, %1, %2" : "=r" (result) : "r" (op1), "r" (op2) );
  return(result);
}

__attribute__( ( always_inline ) ) static __INLINE uint32_t __UASX(uint32_t op1, uint32_t op2)
{
  uint32_t result;
  
  __ASM volatile ("uasx %0, %1, %2" : "=r" (result) : "r" (op1), "r" (op2) );
  return(result);
}

__attribute__( ( always_inline ) ) static __INLINE uint32_t __UQASX(uint32_t op1, uint32_t op2)
{
  uint32_t result;
  
  __ASM volatile ("uqasx %0, %1, %2" : "=r" (result) : "r" (op1), "r" (op2) );
  return(result);
}

__attribute__( ( always_inline ) ) static __INLINE uint32_t __UHASX(uint32_t op1, uint32_t op2)
{
  uint32_t result;
  
  __ASM volatile ("uhasx %0, %1, %2" : "=r" (result) : "r" (op1), "r" (op2) );
  return(result);
}

__attribute__( ( always_inline ) ) static __INLINE uint32_t __SSAX(uint32_t op1, uint32_t op2)
{
  uint32_t result;
  
  __ASM volatile ("ssax %0, %1, %2" : "=r" (result) : "r" (op1), "r" (op2) );
  return(result);
}

__attribute__( ( always_inline ) ) static __INLINE uint32_t __QSAX(uint32_t op1, uint32_t op2)
{
  uint32_t result;
  
  __ASM volatile ("qsax %0, %1, %2" : "=r" (result) : "r" (op1), "r" (op2) );
  return(result);
}

__attribute__( ( always_inline ) ) static __INLINE uint32_t __SHSAX(uint32_t op1, uint32_t op2)
{
  uint32_t result;
  
  __ASM volatile ("shsax %0, %1, %2" : "=r" (result) : "r" (op1), "r" (op2) );
  return(result);
}

__attribute__( ( always_inline ) ) static __INLINE uint32_t __USAX(uint32_t op1, uint32_t op2)
{
  uint32_t result;
  
  __ASM volatile ("usax %0, %1, %2" : "=r" (result) : "r" (op1), "r" (op2) );
  return(result);
}

__attribute__( ( always_inline ) ) static __INLINE uint32_t __UQSAX(uint32_t op1, uint32_t op2)
{
  uint32_t result;
  
  __ASM volatile ("uqsax %0, %1, %2" : "=r" (result) : "r" (op1), "r" (op2) );
  return(result);
}

__attribute__( ( always_inline ) ) static __INLINE uint32_t __UHSAX(uint32_t op1, uint32_t op2)
{
  uint32_t result;
  
  __ASM volatile ("uhsax %0, %1, %2" : "=r" (result) : "r" (op1), "r" (op2) );
  return(result);
}

__attribute__( ( always_inline ) ) static __INLINE uint32_t __USAD8(uint32_t op1, uint32_t op2)
{
  uint32_t result;
  
  __ASM volatile ("usad8 %0, %1, %2" : "=r" (result) : "r" (op1), "r" (op2) );
  return(result);
}

__attribute__( ( always_inline ) ) static __INLINE uint32_t __USADA8(uint32_t op1, uint32_t op2, uint32_t op3)
{
  uint32_t result;
  
  __ASM volatile ("usada8 %0, %1, %2, %3" : "=r" (result) : "r" (op1), "r" (op2), "r" (op3) );
  return(result);
}

#define __SSAT16(ARG1,ARG2) \
({                          \
  uint32_t __RES, __ARG1 = (ARG1); \
  __ASM ("ssat16 %0, %1, %2" : "=r" (__RES) :  "I" (ARG2), "r" (__ARG1) ); \
  __RES; \
 })
  
#define __USAT16(ARG1,ARG2) \
({                          \
  uint32_t __RES, __ARG1 = (ARG1); \
  __ASM ("usat16 %0, %1, %2" : "=r" (__RES) :  "I" (ARG2), "r" (__ARG1) ); \
  __RES; \
 })

__attribute__( ( always_inline ) ) static __INLINE uint32_t __UXTB16(uint32_t op1)
{
  uint32_t result;
  
  __ASM volatile ("uxtb16 %0, %1" : "=r" (result) : "r" (op1));
  return(result);
}

__attribute__( ( always_inline ) ) static __INLINE uint32_t __UXTAB16(uint32_t op1, uint32_t op2)
{
  uint32_t result;
  
  __ASM volatile ("uxtab16 %0, %1, %2" : "=r" (result) : "r" (op1), "r" (op2) );
  return(result);
}

__attribute__( ( always_inline ) ) static __INLINE uint32_t __SXTB16(uint32_t op1)
{
  uint32_t result;
  
  __ASM volatile ("sxtb16 %0, %1" : "=r" (result) : "r" (op1));
  return(result);
}

__attribute__( ( always_inline ) ) static __INLINE uint32_t __SXTAB16(uint32_t op1, uint32_t op2)
{
  uint32_t result;
  
  __ASM volatile ("sxtab16 %0, %1, %2" : "=r" (result) : "r" (op1), "r" (op2) );
  return(result);
}

__attribute__( ( always_inline ) ) static __INLINE uint32_t __SMUAD  (uint32_t op1, uint32_t op2)
{
  uint32_t result;
  
  __ASM volatile ("smuad %0, %1, %2" : "=r" (result) : "r" (op1), "r" (op2) );
  return(result);
}

__attribute__( ( always_inline ) ) static __INLINE uint32_t __SMUADX (uint32_t op1, uint32_t op2)
{
  uint32_t result;
  
  __ASM volatile ("smuadx %0, %1, %2" : "=r" (result) : "r" (op1), "r" (op2) );
  return(result);
}

__attribute__( ( always_inline ) ) static __INLINE uint32_t __SMLAD (uint32_t op1, uint32_t op2, uint32_t op3)
{
  uint32_t result;
  
  __ASM volatile ("smlad %0, %1, %2, %3" : "=r" (result) : "r" (op1), "r" (op2), "r" (op3) );
  return(result);
}

__attribute__( ( always_inline ) ) static __INLINE uint32_t __SMLADX (uint32_t op1, uint32_t op2, uint32_t op3)
{
  uint32_t result;
  
  __ASM volatile ("smladx %0, %1, %2, %3" : "=r" (result) : "r" (op1), "r" (op2), "r" (op3) );
  return(result);
}

#define __SMLALD(ARG1,ARG2,ARG3) \
({ \
  uint32_t __ARG1 = (ARG1), __ARG2 = (ARG2), __ARG3_H = (uint32_t)((uint64_t)(ARG3) >> 32), __ARG3_L = (uint32_t)((uint64_t)(ARG3) & 0xFFFFFFFFUL); \
  __ASM volatile ("smlald %0, %1, %2, %3" : "=r" (__ARG3_L), "=r" (__ARG3_H) : "r" (__ARG1), "r" (__ARG2), "0" (__ARG3_L), "1" (__ARG3_H) ); \
  (uint64_t)(((uint64_t)__ARG3_H << 32) | __ARG3_L); \
 })

#define __SMLALDX(ARG1,ARG2,ARG3) \
({ \
  uint32_t __ARG1 = (ARG1), __ARG2 = (ARG2), __ARG3_H = (uint32_t)((uint64_t)(ARG3) >> 32), __ARG3_L = (uint32_t)((uint64_t)(ARG3) & 0xFFFFFFFFUL); \
  __ASM volatile ("smlaldx %0, %1, %2, %3" : "=r" (__ARG3_L), "=r" (__ARG3_H) : "r" (__ARG1), "r" (__ARG2), "0" (__ARG3_L), "1" (__ARG3_H) ); \
  (uint64_t)(((uint64_t)__ARG3_H << 32) | __ARG3_L); \
 })

__attribute__( ( always_inline ) ) static __INLINE uint32_t __SMUSD  (uint32_t op1, uint32_t op2)
{
  uint32_t result;
  
  __ASM volatile ("smusd %0, %1, %2" : "=r" (result) : "r" (op1), "r" (op2) );
  return(result);
}

__attribute__( ( always_inline ) ) static __INLINE uint32_t __SMUSDX (uint32_t op1, uint32_t op2)
{
  uint32_t result;
  
  __ASM volatile ("smusdx %0, %1, %2" : "=r" (result) : "r" (op1), "r" (op2) );
  return(result);
}

__attribute__( ( always_inline ) ) static __INLINE uint32_t __SMLSD (uint32_t op1, uint32_t op2, uint32_t op3)
{
  uint32_t result;
  
  __ASM volatile ("smlsd %0, %1, %2, %3" : "=r" (result) : "r" (op1), "r" (op2), "r" (op3) );
  return(result);
}

__attribute__( ( always_inline ) ) static __INLINE uint32_t __SMLSDX (uint32_t op1, uint32_t op2, uint32_t op3)
{
  uint32_t result;
  
  __ASM volatile ("smlsdx %0, %1, %2, %3" : "=r" (result) : "r" (op1), "r" (op2), "r" (op3) );
  return(result);
}

#define __SMLSLD(ARG1,ARG2,ARG3) \
({ \
  uint32_t __ARG1 = (ARG1), __ARG2 = (ARG2), __ARG3_H = (uint32_t)((ARG3) >> 32), __ARG3_L = (uint32_t)((ARG3) & 0xFFFFFFFFUL); \
  __ASM volatile ("smlsld %0, %1, %2, %3" : "=r" (__ARG3_L), "=r" (__ARG3_H) : "r" (__ARG1), "r" (__ARG2), "0" (__ARG3_L), "1" (__ARG3_H) ); \
  (uint64_t)(((uint64_t)__ARG3_H << 32) | __ARG3_L); \
 })

#define __SMLSLDX(ARG1,ARG2,ARG3) \
({ \
  uint32_t __ARG1 = (ARG1), __ARG2 = (ARG2), __ARG3_H = (uint32_t)((ARG3) >> 32), __ARG3_L = (uint32_t)((ARG3) & 0xFFFFFFFFUL); \
  __ASM volatile ("smlsldx %0, %1, %2, %3" : "=r" (__ARG3_L), "=r" (__ARG3_H) : "r" (__ARG1), "r" (__ARG2), "0" (__ARG3_L), "1" (__ARG3_H) ); \
  (uint64_t)(((uint64_t)__ARG3_H << 32) | __ARG3_L); \
 })

__attribute__( ( always_inline ) ) static __INLINE uint32_t __SEL  (uint32_t op1, uint32_t op2)
{
  uint32_t result;
  
  __ASM volatile ("sel %0, %1, %2" : "=r" (result) : "r" (op1), "r" (op2) );
  return(result);
}

__attribute__( ( always_inline ) ) static __INLINE uint32_t __QADD(uint32_t op1, uint32_t op2)
{
  uint32_t result;

  __ASM volatile ("qadd %0, %1, %2" : "=r" (result) : "r" (op1), "r" (op2) );
  return(result);
}

__attribute__( ( always_inline ) ) static __INLINE uint32_t __QSUB(uint32_t op1, uint32_t op2)
{
  uint32_t result;
  
  __ASM volatile ("qsub %0, %1, %2" : "=r" (result) : "r" (op1), "r" (op2) );
  return(result);
}

#define __PKHBT(ARG1,ARG2,ARG3) \
({                          \
  uint32_t __RES, __ARG1 = (ARG1), __ARG2 = (ARG2); \
  __ASM ("pkhbt %0, %1, %2, lsl %3" : "=r" (__RES) :  "r" (__ARG1), "r" (__ARG2), "I" (ARG3)  ); \
  __RES; \
 })

#define __PKHTB(ARG1,ARG2,ARG3) \
({                          \
  uint32_t __RES, __ARG1 = (ARG1), __ARG2 = (ARG2); \
  if (ARG3 == 0) \
    __ASM ("pkhtb %0, %1, %2" : "=r" (__RES) :  "r" (__ARG1), "r" (__ARG2)  ); \
  else	\
    __ASM ("pkhtb %0, %1, %2, asr %3" : "=r" (__RES) :  "r" (__ARG1), "r" (__ARG2), "I" (ARG3)  ); \
  __RES; \
 })

/*-- End CM4 SIMD Intrinsics -----------------------------------------------------*/



#elif defined ( __TASKING__ ) /*------------------ TASKING Compiler --------------*/
/* TASKING carm specific functions */


/*------ CM4 SIMD Intrinsics -----------------------------------------------------*/
/* not yet supported */
/*-- End CM4 SIMD Intrinsics -----------------------------------------------------*/


#endif

/*@} end of group CMSIS_SIMD_intrinsics */


#endif /* __CORE_CM4_SIMD_H */

#ifdef __cplusplus
}
#endif
