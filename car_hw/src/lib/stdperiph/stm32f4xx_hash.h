/**
  ******************************************************************************
  * @file    stm32f4xx_hash.h
  * @author  MCD Application Team
  * @version V1.0.0
  * @date    30-September-2011
  * @brief   This file contains all the functions prototypes for the HASH 
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
#ifndef __STM32F4xx_HASH_H
#define __STM32F4xx_HASH_H

#ifdef __cplusplus
 extern "C" {
#endif

/* Includes ------------------------------------------------------------------*/
#include "stm32f4xx.h"

/** @addtogroup STM32F4xx_StdPeriph_Driver
  * @{
  */

/** @addtogroup HASH
  * @{
  */ 

/* Exported types ------------------------------------------------------------*/

/** 
  * @brief   HASH Init structure definition
  */ 
typedef struct
{
  uint32_t HASH_AlgoSelection; /*!< SHA-1 or MD5. This parameter can be a value 
                                    of @ref HASH_Algo_Selection */
  uint32_t HASH_AlgoMode;      /*!< HASH or HMAC. This parameter can be a value 
                                    of @ref HASH_processor_Algorithm_Mode */
  uint32_t HASH_DataType;      /*!< 32-bit data, 16-bit data, 8-bit data or 
                                    bit-string. This parameter can be a value of
                                    @ref HASH_Data_Type */
  uint32_t HASH_HMACKeyType;   /*!< HMAC Short key or HMAC Long Key. This parameter
                                    can be a value of @ref HASH_HMAC_Long_key_only_for_HMAC_mode */
}HASH_InitTypeDef;

/** 
  * @brief  HASH message digest result structure definition  
  */ 
typedef struct
{
  uint32_t Data[5];      /*!< Message digest result : 5x 32bit words for SHA1 or 
                                                      4x 32bit words for MD5  */
} HASH_MsgDigest; 

/** 
  * @brief  HASH context swapping structure definition  
  */ 
typedef struct
{
  uint32_t HASH_IMR; 
  uint32_t HASH_STR;      
  uint32_t HASH_CR;     
  uint32_t HASH_CSR[51];       
}HASH_Context;

/* Exported constants --------------------------------------------------------*/

/** @defgroup HASH_Exported_Constants
  * @{
  */ 

/** @defgroup HASH_Algo_Selection 
  * @{
  */ 
#define HASH_AlgoSelection_SHA1    ((uint16_t)0x0000) /*!< HASH function is SHA1 */
#define HASH_AlgoSelection_MD5     ((uint16_t)0x0080) /*!< HASH function is MD5 */

#define IS_HASH_ALGOSELECTION(ALGOSELECTION) (((ALGOSELECTION) == HASH_AlgoSelection_SHA1) || \
                                              ((ALGOSELECTION) == HASH_AlgoSelection_MD5))
/**
  * @}
  */

/** @defgroup HASH_processor_Algorithm_Mode 
  * @{
  */ 
#define HASH_AlgoMode_HASH         ((uint16_t)0x0000) /*!< Algorithm is HASH */ 
#define HASH_AlgoMode_HMAC         ((uint16_t)0x0040) /*!< Algorithm is HMAC */

#define IS_HASH_ALGOMODE(ALGOMODE) (((ALGOMODE) == HASH_AlgoMode_HASH) || \
                                    ((ALGOMODE) == HASH_AlgoMode_HMAC))
/**
  * @}
  */

/** @defgroup HASH_Data_Type  
  * @{
  */  
#define HASH_DataType_32b          ((uint16_t)0x0000)
#define HASH_DataType_16b          ((uint16_t)0x0010)
#define HASH_DataType_8b           ((uint16_t)0x0020)
#define HASH_DataType_1b           ((uint16_t)0x0030)

#define IS_HASH_DATATYPE(DATATYPE) (((DATATYPE) == HASH_DataType_32b)|| \
                                    ((DATATYPE) == HASH_DataType_16b)|| \
                                    ((DATATYPE) == HASH_DataType_8b)|| \
                                    ((DATATYPE) == HASH_DataType_1b))
/**
  * @}
  */

/** @defgroup HASH_HMAC_Long_key_only_for_HMAC_mode  
  * @{
  */ 
#define HASH_HMACKeyType_ShortKey      ((uint32_t)0x00000000) /*!< HMAC Key is <= 64 bytes */
#define HASH_HMACKeyType_LongKey       ((uint32_t)0x00010000) /*!< HMAC Key is > 64 bytes */

#define IS_HASH_HMAC_KEYTYPE(KEYTYPE) (((KEYTYPE) == HASH_HMACKeyType_ShortKey) || \
                                  ((KEYTYPE) == HASH_HMACKeyType_LongKey))
/**
  * @}
  */

/** @defgroup Number_of_valid_bits_in_last_word_of_the_message   
  * @{
  */  
#define IS_HASH_VALIDBITSNUMBER(VALIDBITS) ((VALIDBITS) <= 0x1F)

/**
  * @}
  */

/** @defgroup HASH_interrupts_definition   
  * @{
  */  
#define HASH_IT_DINI               ((uint8_t)0x01)  /*!< A new block can be entered into the input buffer (DIN)*/
#define HASH_IT_DCI                ((uint8_t)0x02)  /*!< Digest calculation complete */

#define IS_HASH_IT(IT) ((((IT) & (uint8_t)0xFC) == 0x00) && ((IT) != 0x00))
#define IS_HASH_GET_IT(IT) (((IT) == HASH_IT_DINI) || ((IT) == HASH_IT_DCI))
				   
/**
  * @}
  */

/** @defgroup HASH_flags_definition   
  * @{
  */  
#define HASH_FLAG_DINIS            ((uint16_t)0x0001)  /*!< 16 locations are free in the DIN : A new block can be entered into the input buffer.*/
#define HASH_FLAG_DCIS             ((uint16_t)0x0002)  /*!< Digest calculation complete */
#define HASH_FLAG_DMAS             ((uint16_t)0x0004)  /*!< DMA interface is enabled (DMAE=1) or a transfer is ongoing */
#define HASH_FLAG_BUSY             ((uint16_t)0x0008)  /*!< The hash core is Busy : processing a block of data */
#define HASH_FLAG_DINNE            ((uint16_t)0x1000)  /*!< DIN not empty : The input buffer contains at least one word of data */

#define IS_HASH_GET_FLAG(FLAG) (((FLAG) == HASH_FLAG_DINIS) || \
                                ((FLAG) == HASH_FLAG_DCIS)  || \
                                ((FLAG) == HASH_FLAG_DMAS)  || \
                                ((FLAG) == HASH_FLAG_BUSY)  || \
                                ((FLAG) == HASH_FLAG_DINNE)) 

#define IS_HASH_CLEAR_FLAG(FLAG)(((FLAG) == HASH_FLAG_DINIS) || \
                                 ((FLAG) == HASH_FLAG_DCIS))                                 

/**
  * @}
  */ 

/**
  * @}
  */ 

/* Exported macro ------------------------------------------------------------*/
/* Exported functions --------------------------------------------------------*/ 
  
/*  Function used to set the HASH configuration to the default reset state ****/
void HASH_DeInit(void);

/* HASH Configuration function ************************************************/
void HASH_Init(HASH_InitTypeDef* HASH_InitStruct);
void HASH_StructInit(HASH_InitTypeDef* HASH_InitStruct);
void HASH_Reset(void);

/* HASH Message Digest generation functions ***********************************/
void HASH_DataIn(uint32_t Data);
uint8_t HASH_GetInFIFOWordsNbr(void);
void HASH_SetLastWordValidBitsNbr(uint16_t ValidNumber);
void HASH_StartDigest(void);
void HASH_GetDigest(HASH_MsgDigest* HASH_MessageDigest);

/* HASH Context swapping functions ********************************************/
void HASH_SaveContext(HASH_Context* HASH_ContextSave);
void HASH_RestoreContext(HASH_Context* HASH_ContextRestore);

/* HASH's DMA interface function **********************************************/
void HASH_DMACmd(FunctionalState NewState);

/* HASH Interrupts and flags management functions *****************************/
void HASH_ITConfig(uint8_t HASH_IT, FunctionalState NewState);
FlagStatus HASH_GetFlagStatus(uint16_t HASH_FLAG);
void HASH_ClearFlag(uint16_t HASH_FLAG);
ITStatus HASH_GetITStatus(uint8_t HASH_IT);
void HASH_ClearITPendingBit(uint8_t HASH_IT);

/* High Level SHA1 functions **************************************************/
ErrorStatus HASH_SHA1(uint8_t *Input, uint32_t Ilen, uint8_t Output[20]);
ErrorStatus HMAC_SHA1(uint8_t *Key, uint32_t Keylen,
                      uint8_t *Input, uint32_t Ilen,
                      uint8_t Output[20]);

/* High Level MD5 functions ***************************************************/
ErrorStatus HASH_MD5(uint8_t *Input, uint32_t Ilen, uint8_t Output[16]);
ErrorStatus HMAC_MD5(uint8_t *Key, uint32_t Keylen,
                     uint8_t *Input, uint32_t Ilen,
                     uint8_t Output[16]);

#ifdef __cplusplus
}
#endif

#endif /*__STM32F4xx_HASH_H */

/**
  * @}
  */ 

/**
  * @}
  */ 

/******************* (C) COPYRIGHT 2011 STMicroelectronics *****END OF FILE****/
