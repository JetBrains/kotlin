/**
  ******************************************************************************
  * @file    stm32f4xx_usart.h
  * @author  MCD Application Team
  * @version V1.0.0
  * @date    30-September-2011
  * @brief   This file contains all the functions prototypes for the USART 
  *          firmware library.
  ******************************************************************************
  * @attention
  *
  * THE PRESENT FIRMWARE WHICH IS FOR GUIDANCE ONLY AIMS AT PROVIDING CUSTOMERS
  * WITH CODING INFORMATION REGARDING THEIR PRODUCTS IN ORDER FOR THEM TO SAVE
  * TIME. AS A RESULT, STMICROELECTRONICS SHALL NOT BE HELD LIABLE FOR ANY
  * DIRECT, INDIRECT OR CONSEQUENTIAL DAMAGES WITH RESPECT TO ANY CLAIMS ARISING
  * FROM THE CONTENT OF SUCH FIRMWARE AND/OR THE USE MADE BY CUSTOMERS OF THE
  * CODING INFORMATION CONTAINED HEREIN IN CONNECTION WITH THEIR PRODUCTS.
  *
  * <h2><center>&copy; COPYRIGHT 2011 STMicroelectronics</center></h2>
  ******************************************************************************  
  */ 

/* Define to prevent recursive inclusion -------------------------------------*/
#ifndef __STM32F4xx_USART_H
#define __STM32F4xx_USART_H

#ifdef __cplusplus
 extern "C" {
#endif

/* Includes ------------------------------------------------------------------*/
#include "stm32f4xx.h"

/** @addtogroup STM32F4xx_StdPeriph_Driver
  * @{
  */

/** @addtogroup USART
  * @{
  */ 

/* Exported types ------------------------------------------------------------*/ 

/** 
  * @brief  USART Init Structure definition  
  */ 
  
typedef struct
{
  uint32_t USART_BaudRate;            /*!< This member configures the USART communication baud rate.
                                           The baud rate is computed using the following formula:
                                            - IntegerDivider = ((PCLKx) / (8 * (OVR8+1) * (USART_InitStruct->USART_BaudRate)))
                                            - FractionalDivider = ((IntegerDivider - ((u32) IntegerDivider)) * 8 * (OVR8+1)) + 0.5 
                                           Where OVR8 is the "oversampling by 8 mode" configuration bit in the CR1 register. */

  uint16_t USART_WordLength;          /*!< Specifies the number of data bits transmitted or received in a frame.
                                           This parameter can be a value of @ref USART_Word_Length */

  uint16_t USART_StopBits;            /*!< Specifies the number of stop bits transmitted.
                                           This parameter can be a value of @ref USART_Stop_Bits */

  uint16_t USART_Parity;              /*!< Specifies the parity mode.
                                           This parameter can be a value of @ref USART_Parity
                                           @note When parity is enabled, the computed parity is inserted
                                                 at the MSB position of the transmitted data (9th bit when
                                                 the word length is set to 9 data bits; 8th bit when the
                                                 word length is set to 8 data bits). */
 
  uint16_t USART_Mode;                /*!< Specifies wether the Receive or Transmit mode is enabled or disabled.
                                           This parameter can be a value of @ref USART_Mode */

  uint16_t USART_HardwareFlowControl; /*!< Specifies wether the hardware flow control mode is enabled
                                           or disabled.
                                           This parameter can be a value of @ref USART_Hardware_Flow_Control */
} USART_InitTypeDef;

/** 
  * @brief  USART Clock Init Structure definition  
  */ 
  
typedef struct
{

  uint16_t USART_Clock;   /*!< Specifies whether the USART clock is enabled or disabled.
                               This parameter can be a value of @ref USART_Clock */

  uint16_t USART_CPOL;    /*!< Specifies the steady state of the serial clock.
                               This parameter can be a value of @ref USART_Clock_Polarity */

  uint16_t USART_CPHA;    /*!< Specifies the clock transition on which the bit capture is made.
                               This parameter can be a value of @ref USART_Clock_Phase */

  uint16_t USART_LastBit; /*!< Specifies whether the clock pulse corresponding to the last transmitted
                               data bit (MSB) has to be output on the SCLK pin in synchronous mode.
                               This parameter can be a value of @ref USART_Last_Bit */
} USART_ClockInitTypeDef;

/* Exported constants --------------------------------------------------------*/

/** @defgroup USART_Exported_Constants
  * @{
  */ 
  
#define IS_USART_ALL_PERIPH(PERIPH) (((PERIPH) == USART1) || \
                                     ((PERIPH) == USART2) || \
                                     ((PERIPH) == USART3) || \
                                     ((PERIPH) == UART4)  || \
                                     ((PERIPH) == UART5)  || \
                                     ((PERIPH) == USART6))

#define IS_USART_1236_PERIPH(PERIPH) (((PERIPH) == USART1) || \
                                      ((PERIPH) == USART2) || \
                                      ((PERIPH) == USART3) || \
                                      ((PERIPH) == USART6))

/** @defgroup USART_Word_Length 
  * @{
  */ 
  
#define USART_WordLength_8b                  ((uint16_t)0x0000)
#define USART_WordLength_9b                  ((uint16_t)0x1000)
                                    
#define IS_USART_WORD_LENGTH(LENGTH) (((LENGTH) == USART_WordLength_8b) || \
                                      ((LENGTH) == USART_WordLength_9b))
/**
  * @}
  */ 

/** @defgroup USART_Stop_Bits 
  * @{
  */ 
  
#define USART_StopBits_1                     ((uint16_t)0x0000)
#define USART_StopBits_0_5                   ((uint16_t)0x1000)
#define USART_StopBits_2                     ((uint16_t)0x2000)
#define USART_StopBits_1_5                   ((uint16_t)0x3000)
#define IS_USART_STOPBITS(STOPBITS) (((STOPBITS) == USART_StopBits_1) || \
                                     ((STOPBITS) == USART_StopBits_0_5) || \
                                     ((STOPBITS) == USART_StopBits_2) || \
                                     ((STOPBITS) == USART_StopBits_1_5))
/**
  * @}
  */ 

/** @defgroup USART_Parity 
  * @{
  */ 
  
#define USART_Parity_No                      ((uint16_t)0x0000)
#define USART_Parity_Even                    ((uint16_t)0x0400)
#define USART_Parity_Odd                     ((uint16_t)0x0600) 
#define IS_USART_PARITY(PARITY) (((PARITY) == USART_Parity_No) || \
                                 ((PARITY) == USART_Parity_Even) || \
                                 ((PARITY) == USART_Parity_Odd))
/**
  * @}
  */ 

/** @defgroup USART_Mode 
  * @{
  */ 
  
#define USART_Mode_Rx                        ((uint16_t)0x0004)
#define USART_Mode_Tx                        ((uint16_t)0x0008)
#define IS_USART_MODE(MODE) ((((MODE) & (uint16_t)0xFFF3) == 0x00) && ((MODE) != (uint16_t)0x00))
/**
  * @}
  */ 

/** @defgroup USART_Hardware_Flow_Control 
  * @{
  */ 
#define USART_HardwareFlowControl_None       ((uint16_t)0x0000)
#define USART_HardwareFlowControl_RTS        ((uint16_t)0x0100)
#define USART_HardwareFlowControl_CTS        ((uint16_t)0x0200)
#define USART_HardwareFlowControl_RTS_CTS    ((uint16_t)0x0300)
#define IS_USART_HARDWARE_FLOW_CONTROL(CONTROL)\
                              (((CONTROL) == USART_HardwareFlowControl_None) || \
                               ((CONTROL) == USART_HardwareFlowControl_RTS) || \
                               ((CONTROL) == USART_HardwareFlowControl_CTS) || \
                               ((CONTROL) == USART_HardwareFlowControl_RTS_CTS))
/**
  * @}
  */ 

/** @defgroup USART_Clock 
  * @{
  */ 
#define USART_Clock_Disable                  ((uint16_t)0x0000)
#define USART_Clock_Enable                   ((uint16_t)0x0800)
#define IS_USART_CLOCK(CLOCK) (((CLOCK) == USART_Clock_Disable) || \
                               ((CLOCK) == USART_Clock_Enable))
/**
  * @}
  */ 

/** @defgroup USART_Clock_Polarity 
  * @{
  */
  
#define USART_CPOL_Low                       ((uint16_t)0x0000)
#define USART_CPOL_High                      ((uint16_t)0x0400)
#define IS_USART_CPOL(CPOL) (((CPOL) == USART_CPOL_Low) || ((CPOL) == USART_CPOL_High))

/**
  * @}
  */ 

/** @defgroup USART_Clock_Phase
  * @{
  */

#define USART_CPHA_1Edge                     ((uint16_t)0x0000)
#define USART_CPHA_2Edge                     ((uint16_t)0x0200)
#define IS_USART_CPHA(CPHA) (((CPHA) == USART_CPHA_1Edge) || ((CPHA) == USART_CPHA_2Edge))

/**
  * @}
  */

/** @defgroup USART_Last_Bit
  * @{
  */

#define USART_LastBit_Disable                ((uint16_t)0x0000)
#define USART_LastBit_Enable                 ((uint16_t)0x0100)
#define IS_USART_LASTBIT(LASTBIT) (((LASTBIT) == USART_LastBit_Disable) || \
                                   ((LASTBIT) == USART_LastBit_Enable))
/**
  * @}
  */ 

/** @defgroup USART_Interrupt_definition 
  * @{
  */
  
#define USART_IT_PE                          ((uint16_t)0x0028)
#define USART_IT_TXE                         ((uint16_t)0x0727)
#define USART_IT_TC                          ((uint16_t)0x0626)
#define USART_IT_RXNE                        ((uint16_t)0x0525)
#define USART_IT_ORE_RX                      ((uint16_t)0x0325) /* In case interrupt is generated if the RXNEIE bit is set */
#define USART_IT_IDLE                        ((uint16_t)0x0424)
#define USART_IT_LBD                         ((uint16_t)0x0846)
#define USART_IT_CTS                         ((uint16_t)0x096A)
#define USART_IT_ERR                         ((uint16_t)0x0060)
#define USART_IT_ORE_ER                      ((uint16_t)0x0360) /* In case interrupt is generated if the EIE bit is set */
#define USART_IT_NE                          ((uint16_t)0x0260)
#define USART_IT_FE                          ((uint16_t)0x0160)

/** @defgroup USART_Legacy 
  * @{
  */
#define USART_IT_ORE                          USART_IT_ORE_ER               
/**
  * @}
  */

#define IS_USART_CONFIG_IT(IT) (((IT) == USART_IT_PE) || ((IT) == USART_IT_TXE) || \
                                ((IT) == USART_IT_TC) || ((IT) == USART_IT_RXNE) || \
                                ((IT) == USART_IT_IDLE) || ((IT) == USART_IT_LBD) || \
                                ((IT) == USART_IT_CTS) || ((IT) == USART_IT_ERR))
#define IS_USART_GET_IT(IT) (((IT) == USART_IT_PE) || ((IT) == USART_IT_TXE) || \
                             ((IT) == USART_IT_TC) || ((IT) == USART_IT_RXNE) || \
                             ((IT) == USART_IT_IDLE) || ((IT) == USART_IT_LBD) || \
                             ((IT) == USART_IT_CTS) || ((IT) == USART_IT_ORE) || \
                             ((IT) == USART_IT_ORE_RX) || ((IT) == USART_IT_ORE_ER) || \
                             ((IT) == USART_IT_NE) || ((IT) == USART_IT_FE))
#define IS_USART_CLEAR_IT(IT) (((IT) == USART_IT_TC) || ((IT) == USART_IT_RXNE) || \
                               ((IT) == USART_IT_LBD) || ((IT) == USART_IT_CTS))
/**
  * @}
  */

/** @defgroup USART_DMA_Requests 
  * @{
  */

#define USART_DMAReq_Tx                      ((uint16_t)0x0080)
#define USART_DMAReq_Rx                      ((uint16_t)0x0040)
#define IS_USART_DMAREQ(DMAREQ) ((((DMAREQ) & (uint16_t)0xFF3F) == 0x00) && ((DMAREQ) != (uint16_t)0x00))

/**
  * @}
  */ 

/** @defgroup USART_WakeUp_methods
  * @{
  */

#define USART_WakeUp_IdleLine                ((uint16_t)0x0000)
#define USART_WakeUp_AddressMark             ((uint16_t)0x0800)
#define IS_USART_WAKEUP(WAKEUP) (((WAKEUP) == USART_WakeUp_IdleLine) || \
                                 ((WAKEUP) == USART_WakeUp_AddressMark))
/**
  * @}
  */

/** @defgroup USART_LIN_Break_Detection_Length 
  * @{
  */
  
#define USART_LINBreakDetectLength_10b      ((uint16_t)0x0000)
#define USART_LINBreakDetectLength_11b      ((uint16_t)0x0020)
#define IS_USART_LIN_BREAK_DETECT_LENGTH(LENGTH) \
                               (((LENGTH) == USART_LINBreakDetectLength_10b) || \
                                ((LENGTH) == USART_LINBreakDetectLength_11b))
/**
  * @}
  */

/** @defgroup USART_IrDA_Low_Power 
  * @{
  */

#define USART_IrDAMode_LowPower              ((uint16_t)0x0004)
#define USART_IrDAMode_Normal                ((uint16_t)0x0000)
#define IS_USART_IRDA_MODE(MODE) (((MODE) == USART_IrDAMode_LowPower) || \
                                  ((MODE) == USART_IrDAMode_Normal))
/**
  * @}
  */ 

/** @defgroup USART_Flags 
  * @{
  */

#define USART_FLAG_CTS                       ((uint16_t)0x0200)
#define USART_FLAG_LBD                       ((uint16_t)0x0100)
#define USART_FLAG_TXE                       ((uint16_t)0x0080)
#define USART_FLAG_TC                        ((uint16_t)0x0040)
#define USART_FLAG_RXNE                      ((uint16_t)0x0020)
#define USART_FLAG_IDLE                      ((uint16_t)0x0010)
#define USART_FLAG_ORE                       ((uint16_t)0x0008)
#define USART_FLAG_NE                        ((uint16_t)0x0004)
#define USART_FLAG_FE                        ((uint16_t)0x0002)
#define USART_FLAG_PE                        ((uint16_t)0x0001)
#define IS_USART_FLAG(FLAG) (((FLAG) == USART_FLAG_PE) || ((FLAG) == USART_FLAG_TXE) || \
                             ((FLAG) == USART_FLAG_TC) || ((FLAG) == USART_FLAG_RXNE) || \
                             ((FLAG) == USART_FLAG_IDLE) || ((FLAG) == USART_FLAG_LBD) || \
                             ((FLAG) == USART_FLAG_CTS) || ((FLAG) == USART_FLAG_ORE) || \
                             ((FLAG) == USART_FLAG_NE) || ((FLAG) == USART_FLAG_FE))
                              
#define IS_USART_CLEAR_FLAG(FLAG) ((((FLAG) & (uint16_t)0xFC9F) == 0x00) && ((FLAG) != (uint16_t)0x00))

#define IS_USART_BAUDRATE(BAUDRATE) (((BAUDRATE) > 0) && ((BAUDRATE) < 7500001))
#define IS_USART_ADDRESS(ADDRESS) ((ADDRESS) <= 0xF)
#define IS_USART_DATA(DATA) ((DATA) <= 0x1FF)

/**
  * @}
  */ 

/**
  * @}
  */ 

/* Exported macro ------------------------------------------------------------*/
/* Exported functions --------------------------------------------------------*/  

/*  Function used to set the USART configuration to the default reset state ***/ 
void USART_DeInit(USART_TypeDef* USARTx);

/* Initialization and Configuration functions *********************************/
void USART_Init(USART_TypeDef* USARTx, USART_InitTypeDef* USART_InitStruct);
void USART_StructInit(USART_InitTypeDef* USART_InitStruct);
void USART_ClockInit(USART_TypeDef* USARTx, USART_ClockInitTypeDef* USART_ClockInitStruct);
void USART_ClockStructInit(USART_ClockInitTypeDef* USART_ClockInitStruct);
void USART_Cmd(USART_TypeDef* USARTx, FunctionalState NewState);
void USART_SetPrescaler(USART_TypeDef* USARTx, uint8_t USART_Prescaler);
void USART_OverSampling8Cmd(USART_TypeDef* USARTx, FunctionalState NewState);
void USART_OneBitMethodCmd(USART_TypeDef* USARTx, FunctionalState NewState);

/* Data transfers functions ***************************************************/ 
void USART_SendData(USART_TypeDef* USARTx, uint16_t Data);
uint16_t USART_ReceiveData(USART_TypeDef* USARTx);

/* Multi-Processor Communication functions ************************************/
void USART_SetAddress(USART_TypeDef* USARTx, uint8_t USART_Address);
void USART_WakeUpConfig(USART_TypeDef* USARTx, uint16_t USART_WakeUp);
void USART_ReceiverWakeUpCmd(USART_TypeDef* USARTx, FunctionalState NewState);

/* LIN mode functions *********************************************************/
void USART_LINBreakDetectLengthConfig(USART_TypeDef* USARTx, uint16_t USART_LINBreakDetectLength);
void USART_LINCmd(USART_TypeDef* USARTx, FunctionalState NewState);
void USART_SendBreak(USART_TypeDef* USARTx);

/* Half-duplex mode function **************************************************/
void USART_HalfDuplexCmd(USART_TypeDef* USARTx, FunctionalState NewState);

/* Smartcard mode functions ***************************************************/
void USART_SmartCardCmd(USART_TypeDef* USARTx, FunctionalState NewState);
void USART_SmartCardNACKCmd(USART_TypeDef* USARTx, FunctionalState NewState);
void USART_SetGuardTime(USART_TypeDef* USARTx, uint8_t USART_GuardTime);

/* IrDA mode functions ********************************************************/
void USART_IrDAConfig(USART_TypeDef* USARTx, uint16_t USART_IrDAMode);
void USART_IrDACmd(USART_TypeDef* USARTx, FunctionalState NewState);

/* DMA transfers management functions *****************************************/
void USART_DMACmd(USART_TypeDef* USARTx, uint16_t USART_DMAReq, FunctionalState NewState);

/* Interrupts and flags management functions **********************************/
void USART_ITConfig(USART_TypeDef* USARTx, uint16_t USART_IT, FunctionalState NewState);
FlagStatus USART_GetFlagStatus(USART_TypeDef* USARTx, uint16_t USART_FLAG);
void USART_ClearFlag(USART_TypeDef* USARTx, uint16_t USART_FLAG);
ITStatus USART_GetITStatus(USART_TypeDef* USARTx, uint16_t USART_IT);
void USART_ClearITPendingBit(USART_TypeDef* USARTx, uint16_t USART_IT);

#ifdef __cplusplus
}
#endif

#endif /* __STM32F4xx_USART_H */

/**
  * @}
  */ 

/**
  * @}
  */ 

/******************* (C) COPYRIGHT 2011 STMicroelectronics *****END OF FILE****/
